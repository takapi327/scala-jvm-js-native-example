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

          bms.send(authentication) <* bms.receive.flatMap {
            case res: ResponsePacket if res.isAuthMethodSwitchRequestPacket =>
              Concurrent[F].raiseError(new Exception("Authentication Method Switch Request"))
            case res: ResponsePacket if res.isAuthNextFactorPacket =>
              Concurrent[F].raiseError(new Exception("Authentication Next Factor"))
            case res: ResponsePacket if res.isAuthMoreDataPacket => bms.receive *> Concurrent[F].unit
            case res: ResponsePacket if res.isOKPacket           => Concurrent[F].unit
            case res: ResponsePacket if res.isErrorPacket =>
              Concurrent[F].raiseError(new Exception("Authentication failed"))
            case res: ResponsePacket if res.isEOFPacket => Concurrent[F].raiseError(new Exception("EOF Packet"))
            case _                                      => Concurrent[F].raiseError(new Exception("Unknown packet"))
          }

        override def executeQuery(sql: String): F[Unit] =
          bms.changeCommandPhase *> bms.send(ComQuery(sql))
    }
