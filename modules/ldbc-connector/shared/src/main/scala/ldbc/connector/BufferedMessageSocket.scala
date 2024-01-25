/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.concurrent.duration.Duration

import cats.*
import cats.syntax.all.*

import cats.effect.*
//import cats.effect.std.{ Console, Queue }
import cats.effect.std.Console
//import cats.effect.implicits.*

import fs2.io.net.Socket

import ldbc.connector.net.SSLNegotiation
import ldbc.connector.net.message.Message
import ldbc.connector.net.packet.*

/**
 * A `MessageSocket` that buffers incoming messages, removing and handling asynchronous back-end
 * messages. This splits the protocol into a [logically] synchronous message exchange plus a set of
 * out-of-band broadcast channels that can be observed or ignored at the user's discretion.
 */
trait BufferedMessageSocket[F[_]] extends MessageSocket[F]:

  protected def terminate: F[Unit]

object BufferedMessageSocket:

  /**
   * A poison pill that we place in the message queue to indicate that we're in a fatal error
   * condition, message processing has stopped, and any further attempts to send or receive should
   * result in `cause` being raised.
   */
  //private case class NetworkError(cause: Throwable) extends Packet

  def apply[F[_]: Temporal: Console](
    queueSize:   Int,
    debug:       Boolean,
    sockets:     Resource[F, Socket[F]],
    sslOptions:  Option[SSLNegotiation.Options[F]],
    readTimeout: Duration
  ): Resource[F, BufferedMessageSocket[F]] =
    for
      ms  <- MessageSocket[F](debug, sockets, sslOptions, readTimeout)
      ams <- Resource.make(BufferedMessageSocket.fromMessageSocket[F](ms, queueSize))(_.terminate)
    yield ams

  /**
   * Read one message and handle it if we can, otherwise emit it to the user. This is how we deal
   * with asynchronous messages, and messages that require us to record a bit of information that
   * the user might ask for later.
   */
  //private def next[F[_]: MonadThrow](
  //  ms: MessageSocket[F],
  //  // xaSig: Ref[F, TransactionStatus],
  //  // paSig: Ref[F, Map[String, String]],
  //  // bkDef: Deferred[F, BackendKeyData],
  //  // noTop: Topic[F, Notification[String]],
  //  queue: Queue[F, Packet]
  //): F[Unit] =
  //  def step: F[Unit] = ms.receive.flatMap(packet => queue.offer(packet)) >> step

  //  step.attempt.flatMap {
  //    case Left(e)  => queue.offer(NetworkError(e)) // publish the failure
  //    case Right(_) => Monad[F].unit
  //  }

  /**
   * Here we read messages as they arrive, rather than waiting for the user to ask. This allows us
   * to handle asynchronous messages, which are dealt with here and not passed on. Other messages
   * are queued up and are typically consumed immediately, so a small queue size is probably fine.
   */
  def fromMessageSocket[F[_]: Concurrent: Console](
    ms:        MessageSocket[F],
    queueSize: Int
  ): F[BufferedMessageSocket[F]] =
    for
      term  <- Ref[F].of[Option[Throwable]](None)
      //queue <- Queue.bounded[F, Packet](queueSize)
      //fib   <- next(ms, queue).start
    yield new BufferedMessageSocket[F]:

      override def initialPacket: InitialPacket = ms.initialPacket

      // n.b. there is a race condition here, prevented by the protocol semaphore
      override def receive: F[Packet] =
        term.get.flatMap {
          case Some(t) => Concurrent[F].raiseError(t)
          case None =>
            ms.receive
            //queue.take.flatMap {
            //  case e: NetworkError => term.set(Some(e.cause)) *> receive
            //  case m               => m.pure[F]
            //}
        }

      override def send(message: Message): F[Unit] =
        term.get.flatMap {
          case Some(t) => Concurrent[F].raiseError(t)
          case None    => ms.send(message)
        }

      override protected def terminate: F[Unit] =
        //fib.cancel *> // stop processing incoming messages
          Console[F].println("Terminating")
        //  send(Terminate) // server will close the socket when it sees this

      override def history(max: Int): F[List[Either[Any, Any]]] =
        ms.history(max)

      override def changeCommandPhase: F[Unit] = ms.changeCommandPhase
