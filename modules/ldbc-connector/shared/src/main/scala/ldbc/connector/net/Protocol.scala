/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scala.concurrent.duration.Duration

import org.typelevel.otel4s.trace.Tracer

import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import fs2.io.net.Socket

import scodec.Decoder

import ldbc.connector.BufferedMessageSocket
import ldbc.connector.net.protocol.Exchange
import ldbc.connector.net.message.*
import ldbc.connector.net.packet.*
import ldbc.connector.authenticator.*

/**
 * Interface for a MySQL database, expressed through high-level operations that rely on exchange
 * of multiple messages. Operations here can be executed concurrently and are non-cancelable. The
 * structures returned here expose internals (safely) that are important for error reporting but are
 * not generally useful for end users.
 */
trait Protocol[F[_]]:

  def initialPacket: InitialPacket

  def authenticate(user: String, password: String): F[Unit]

  def executeQuery(sql: String): F[Unit]

object Protocol:

  /**
   * Resource yielding a new `Protocol` with the given `host` and `port`.
   */
  def apply[F[_]: Temporal: Tracer: Console](
    debug:       Boolean,
    sockets:     Resource[F, Socket[F]],
    sslOptions:  Option[SSLNegotiation.Options[F]],
    readTimeout: Duration
  ): Resource[F, Protocol[F]] =
    for
      bms      <- BufferedMessageSocket[F](256, debug, sockets, sslOptions, readTimeout)
      protocol <- Resource.eval(fromMessageSocket[F](bms))
    yield protocol

  def fromMessageSocket[F[_]: Concurrent: Tracer](
    bms: BufferedMessageSocket[F]
  ): F[Protocol[F]] =
    Exchange[F].map { ex =>
      new Protocol[F]:
        override def initialPacket: InitialPacket = bms.initialPacket
        override def authenticate(user: String, password: String): F[Unit] =
          val plugin = initialPacket.authPlugin match
            case "mysql_native_password" => new MysqlNativePasswordPlugin
            case "caching_sha2_password" => CachingSha2PasswordPlugin(Some(password), None)
            case _                       => throw new Exception(s"Unknown plugin: ${ initialPacket.authPlugin }")

          val hashedPassword = plugin.hashPassword(password, initialPacket.scrambleBuff)

          val authentication = Authenticate(user, Array(hashedPassword.length.toByte) ++ hashedPassword, plugin.name)

          bms.send(authentication) <* bms.receive(AuthenticationPacket.decoder).flatMap {
            case res: AuthMoreDataPacket => bms.receive(AuthenticationPacket.decoder)
          }

        def repeatProcess[P <: Packet](times: Int, decoder: Decoder[P]): F[List[P]] =
          def read(remaining: Int, acc: List[P]): F[List[P]] =
            if remaining <= 0 then Concurrent[F].pure(acc)
            else bms.receive(decoder).flatMap(result => read(remaining - 1, acc :+ result))

          read(times, List.empty[P])

        override def executeQuery(sql: String): F[Unit] =
          for
            columnCount <- bms.changeCommandPhase *>
                             bms.send(ComQuery(sql)) *>
                             bms.receive(ColumnsNumberPacket.decoder)
            columns      <- repeatProcess(columnCount.columnCount, ColumnDefinitionPacket.decoder)
            resultSetRow <- bms.receive(ResultSetRowPacket.decoder(columns))
          yield
            println(s"columns: ${ columns.map(_.info).mkString(", ") }")
            println(s"records: ${ resultSetRow.value.mkString(", ") }")
            resultSetRow.value.foreach {
              case value: java.time.LocalTime => println(s"Time: $value")
              case value: java.time.LocalDate => println(s"Date: $value")
              case value: java.time.LocalDateTime => println(s"Timestamp: $value")
              case value: String => println(s"String: $value")
              case value => println(s"Unknown: $value")
            }
    }
