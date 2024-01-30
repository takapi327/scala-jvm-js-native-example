/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import com.comcast.ip4s.*

import cats.*
import cats.data.Kleisli
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.Console

import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.net.*
import ldbc.connector.util.*
import ldbc.connector.exception.LdbcException

trait Session[F[_]]:

  def executeQuery[A](sql: String)(codec: ldbc.connector.Codec[A]): F[List[A]]

  def preparedStatement(sql: String): F[PreparedStatement[F]]

object Session:

  private val DefaultSocketOptions: List[SocketOption] =
    List(SocketOption.noDelay(true))

  abstract class Impl[F[_]: MonadCancelThrow] extends Session[F]

  object Recyclers:

    /**
     * Ensure the session is idle, then remove all channel listeners and reset all variables to
     * system defaults. Note that this is usually more work than you need to do. If your application
     * isn't running arbitrary statements then `minimal` might be more efficient.
     */
    def full[F[_]: Monad]: Recycler[F, Session[F]] =
      // ensureIdle[F] <+> unlistenAll <+> resetAll
      Recycler.success[F, Session[F]]

    /**
     * Yield `true` the session is idle (i.e., that there is no ongoing transaction), otherwise
     * yield false. This check does not require network IO.
     */
    // def ensureIdle[F[_] : Monad]: Recycler[F, Session[F]] =
    //  Recycler(_.transactionStatus.get.map(_ == TransactionStatus.Idle))

    /** Remove all channel listeners and yield `true`. */
    // def unlistenAll[F[_] : Functor]: Recycler[F, Session[F]] =
    //  Recycler(_.execute(Command("UNLISTEN *", Origin.unknown, Void.codec)).as(true))

    /** Reset all variables to system defaults and yield `true`. */
    // def resetAll[F[_] : Functor]: Recycler[F, Session[F]] =
    //  Recycler(_.execute(Command("RESET ALL", Origin.unknown, Void.codec)).as(true))

  def single[F[_]: Temporal: Tracer: Network: Console](
    host:        String,
    port:        Int,
    user:        String,
    password:    Option[String] = None,
    debug:       Boolean = false,
    ssl:         SSL = SSL.None,
    readTimeout: Duration = Duration.Inf
  ): Resource[F, Session[F]] =
    singleTracer(host, port, user, password, debug, ssl, readTimeout).apply(Tracer[F])

  def singleTracer[F[_]: Temporal: Network: Console](
    host:        String,
    port:        Int,
    user:        String,
    password:    Option[String] = None,
    debug:       Boolean = false,
    ssl:         SSL = SSL.None,
    readTimeout: Duration = Duration.Inf
  ): Tracer[F] => Resource[F, Session[F]] =
    Kleisli((_: Tracer[F]) =>
      pooled[F](
        host        = host,
        port        = port,
        user        = user,
        password    = password,
        max         = 1,
        debug       = debug,
        ssl         = ssl,
        readTimeout = readTimeout
      )
    )
      .flatMap(f => Kleisli { implicit T: Tracer[F] => f(T) })
      .run

  def fromSockets[F[_]: Temporal: Tracer: Console](
    sockets:     Resource[F, Socket[F]],
    host:        String,
    port:        Int,
    user:        String,
    password:    Option[String] = None,
    debug:       Boolean = false,
    sslOptions:  Option[SSLNegotiation.Options[F]],
    readTimeout: Duration = Duration.Inf
  ): Resource[F, Session[F]] =
    for
      protocol <- Protocol[F](debug, sockets, sslOptions, readTimeout)
      _        <- Resource.eval(protocol.authenticate(user, password.getOrElse("")))
    yield new Impl[F]:
      override def executeQuery[A](sql: String)(codec: ldbc.connector.Codec[A]): F[List[A]] =
        protocol.executeQuery(sql)(codec)

      override def preparedStatement(sql: String): F[PreparedStatement[F]] = protocol.preparedStatement(sql)

  def fromSocketGroup[F[_]: Tracer: Console](
    socketGroup:   SocketGroup[F],
    host:          String,
    port:          Int,
    user:          String,
    password:      Option[String] = None,
    debug:         Boolean = false,
    socketOptions: List[SocketOption],
    sslOptions:    Option[SSLNegotiation.Options[F]],
    readTimeout:   Duration = Duration.Inf
  )(using ev: Temporal[F]): Resource[F, Session[F]] =
    def fail[A](msg: String): Resource[F, A] =
      Resource.eval(ev.raiseError(new LdbcException(message = msg, sql = None)))

    def socket: Resource[F, Socket[F]] =
      (Hostname.fromString(host), Port.fromInt(port)) match
        case (Some(validHost), Some(validPort)) =>
          socketGroup.client(SocketAddress(validHost, validPort), socketOptions)
        case (None, _) => fail(s"""Hostname: "$host" is not syntactically valid.""")
        case (_, None) => fail(s"Port: $port falls out of the allowed range.")

    fromSockets(socket, host, port, user, password, debug, sslOptions, readTimeout)

  def pooled[F[_]: Temporal: Network: Console](
    host:          String,
    port:          Int,
    user:          String,
    password:      Option[String] = None,
    max:           Int,
    debug:         Boolean = false,
    ssl:           SSL = SSL.None,
    socketOptions: List[SocketOption] = Session.DefaultSocketOptions,
    readTimeout:   Duration = Duration.Inf
  ): Resource[F, Tracer[F] => Resource[F, Session[F]]] =

    val logger: String => F[Unit] = s => Console[F].println(s"TLS: $s")

    def session(socketGroup: SocketGroup[F], sslOp: Option[SSLNegotiation.Options[F]])(using
      Tracer[F]
    ): Resource[F, Session[F]] =
      fromSocketGroup(socketGroup, host, port, user, password, debug, socketOptions, sslOp, readTimeout)

    for
      sslOp <- ssl.toSSLNegotiationOptions(if debug then logger.some else none)
      pool  <- Pool.ofF({ implicit T: Tracer[F] => session(Network[F], sslOp) }, max)(Recyclers.full)
    yield pool
