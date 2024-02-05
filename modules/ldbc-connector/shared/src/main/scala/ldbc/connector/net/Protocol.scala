/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scala.concurrent.duration.Duration
import scala.collection.immutable.ListMap

import org.typelevel.otel4s.trace.Tracer

import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import fs2.io.net.Socket

import scodec.Decoder

import ldbc.connector.{ BufferedMessageSocket, PreparedStatement }
import ldbc.connector.net.protocol.Exchange
import ldbc.connector.net.message.*
import ldbc.connector.net.packet.*
import ldbc.connector.authenticator.*
import ldbc.connector.data.Parameter

/**
 * Interface for a MySQL database, expressed through high-level operations that rely on exchange
 * of multiple messages. Operations here can be executed concurrently and are non-cancelable. The
 * structures returned here expose internals (safely) that are important for error reporting but are
 * not generally useful for end users.
 */
trait Protocol[F[_]]:

  def initialPacket: InitialPacket

  def authenticate(user: String, password: String): F[Unit]

  def executeQuery[A](sql: String)(codec: ldbc.connector.Codec[A]): F[List[A]]

  def clientPreparedStatement(sql: String): F[PreparedStatement.Client[F]]

  def serverPreparedStatement(sql: String): F[PreparedStatement.Server[F]]

  def close(): F[Unit]

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
      protocol <- Resource.make(fromMessageSocket[F](bms))(_.close())
    yield protocol

  def fromMessageSocket[F[_]: Concurrent: Tracer](
    bms: BufferedMessageSocket[F]
  ): F[Protocol[F]] =
    Exchange[F].map { ex =>
      new Protocol[F]:
        override def initialPacket: InitialPacket = bms.initialPacket

        private def readUntilOk(): F[Unit] =
          bms.receive(AuthenticationPacket.decoder).flatMap {
            case _: AuthMoreDataPacket => readUntilOk()
            case _: OKPacket           => Concurrent[F].unit
            case error: ERRPacket =>
              Concurrent[F].raiseError(new Exception(s"Connection error: ${ error.errorMessage }"))
          }

        override def authenticate(user: String, password: String): F[Unit] =
          val plugin = initialPacket.authPlugin match
            case "mysql_native_password" => new MysqlNativePasswordPlugin
            case "caching_sha2_password" => CachingSha2PasswordPlugin(Some(password), None)
            case _                       => throw new Exception(s"Unknown plugin: ${ initialPacket.authPlugin }")

          val hashedPassword = plugin.hashPassword(password, initialPacket.scrambleBuff)

          val authentication = Authenticate(user, Array(hashedPassword.length.toByte) ++ hashedPassword, plugin.name)

          bms.send(authentication) <* readUntilOk()

        def repeatProcess[P <: Packet](times: Int, decoder: Decoder[P]): F[List[P]] =
          def read(remaining: Int, acc: List[P]): F[List[P]] =
            if remaining <= 0 then Concurrent[F].pure(acc)
            else bms.receive(decoder).flatMap(result => read(remaining - 1, acc :+ result))

          read(times, List.empty[P])

        def readUntilEOF(
          columns: List[ColumnDefinitionPacket],
          acc:     List[ResultSetRowPacket]
        ): F[List[ResultSetRowPacket]] =
          bms.receive(ResultSetRowPacket.decoder(columns)).flatMap {
            case _: EOFPacket            => Concurrent[F].pure(acc)
            case row: ResultSetRowPacket => readUntilEOF(columns, acc :+ row)
          }

        override def executeQuery[A](sql: String)(codec: ldbc.connector.Codec[A]): F[List[A]] =
          for
            columnCount <- bms.changeCommandPhase *>
                             bms.send(ComQuery(sql, initialPacket.capabilityFlags, Map.empty)) *>
                             bms.receive(ColumnsNumberPacket.decoder).flatMap {
                               case error: ERRPacket =>
                                 Concurrent[F]
                                   .raiseError(new Exception(s"Failed to execute query: ${ error.errorMessage }"))
                               case result: ColumnsNumberPacket => Concurrent[F].pure(result)
                             }
            columns      <- repeatProcess(columnCount.columnCount, ColumnDefinitionPacket.decoder)
            resultSetRow <- readUntilEOF(columns, Nil)
          yield resultSetRow
            .map(row =>
              codec.decode(0, row.value) match
                case Left(value) =>
                  val column = columns(value.offset)
                  throw new IllegalArgumentException(s"""
                  |==========================
                  |Failed to decode column: `${ column.name }`
                  |Decode To: ${ column.columnType } -> ${ value.`type`.name.toUpperCase }
                  |
                  |Message [ ${ value.message } ]
                  |==========================
                  |""".stripMargin)
                case Right(value) => value
            )

        override def clientPreparedStatement(sql: String): F[PreparedStatement.Client[F]] =
          Ref[F].of(ListMap.empty[Int, Parameter])
            .map(params => PreparedStatement.Client[F](bms, sql, params, initialPacket.capabilityFlags))

        override def serverPreparedStatement(sql: String): F[PreparedStatement.Server[F]] =
          for
            result <- bms.changeCommandPhase *> bms.send(ComStmtPrepare(sql)) *>
                        bms.receive(ComStmtPrepareOkPacket.decoder).flatMap {
                          case error: ERRPacket =>
                            Concurrent[F]
                              .raiseError(new Exception(s"Failed to prepare statement: ${ error.errorMessage }"))
                          case result: ComStmtPrepareOkPacket => Concurrent[F].pure(result)
                        }
            _ <- repeatProcess(result.numParams, ParameterDefinitionPacket.decoder)
            _ <- repeatProcess(result.numColumns, ColumnDefinitionPacket.decoder)
            params <- Ref[F].of(ListMap.empty[Int, Parameter])
          yield PreparedStatement.Server[F](result.statementId, bms, params)

        override def close(): F[Unit] = bms.changeCommandPhase *> bms.send(ComQuit())
    }
