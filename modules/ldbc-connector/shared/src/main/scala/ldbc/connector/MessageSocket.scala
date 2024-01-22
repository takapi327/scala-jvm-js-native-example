/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.io.AnsiColor
import scala.concurrent.duration.Duration

import cats.Applicative
import cats.syntax.all.*

import cats.effect.*
import cats.effect.std.*

import fs2.io.net.Socket

import ldbc.connector.net.{ SSLNegotiation, BitVectorSocket }
import ldbc.connector.net.message.Message
import ldbc.connector.net.packet.*

/**
 * A higher-level `BitVectorSocket` that speaks in terms of `Message`.
 */
trait MessageSocket[F[_]]:

  def initialPacket: InitialPacket

  /**
   * Receive the next `BackendMessage`, or raise an exception if EOF is reached before a complete
   * message arrives.
   */
  def receive: F[Packet]

  /** Send the specified message. */
  def send(message: Message): F[Unit]

  /** Destructively read the last `n` messages from the circular buffer. */
  def history(max: Int): F[List[Either[Any, Any]]]

object MessageSocket:

  def fromBitVectorSocket[F[_]: Concurrent: Console](
    bvs:          BitVectorSocket[F],
    debugEnabled: Boolean
  ): F[MessageSocket[F]] =
    Queue.circularBuffer[F, Either[Any, Any]](10).map { cb =>
      new MessageSocket[F]:

        override def initialPacket: InitialPacket = bvs.initialPacket

        private def debug(msg: => String): F[Unit] =
          if debugEnabled then Console[F].println(msg) else Concurrent[F].unit

        private def parseHeader(headerBytes: Array[Byte]): Int =
          (headerBytes(0) & 0xff) | ((headerBytes(1) & 0xff) << 8) | ((headerBytes(2) & 0xff) << 16)

        /**
         * Messages are prefixed with a 5-byte header consisting of a tag (byte) and a length (int32,
         * total including self but not including the tag) in network order.
         */
        val receiveImpl: F[Packet] =
          (for
            header <- bvs.read(4)
            payloadSize = parseHeader(header.toByteArray)
            payload <- bvs.read(payloadSize)
          yield ResponsePacket(header, payload)).onError {
            case t => debug(s" ← ${ AnsiColor.RED }${ t.getMessage }${ AnsiColor.RESET }")
          }

        override def receive: F[Packet] =
          for
            msg <- receiveImpl
            _   <- cb.offer(Right(msg))
            _   <- debug(s" ← ${ AnsiColor.GREEN }$msg${ AnsiColor.RESET }")
          yield msg

        override def send(message: Message): F[Unit] =
          debug(s" → ${ AnsiColor.YELLOW }$message${ AnsiColor.RESET }") *>
            bvs.write(message.encode) *>
            cb.offer(Left(message))

        override def history(max: Int): F[List[Either[Any, Any]]] =
          cb.take.flatMap { first =>
            def pump(acc: List[Either[Any, Any]]): F[List[Either[Any, Any]]] =
              cb.tryTake.flatMap {
                case Some(e) => pump(e :: acc)
                case None    => Applicative[F].pure(acc.reverse)
              }
            pump(List(first))
          }
    }

  def apply[F[_]: Console: Temporal](
    debug:       Boolean,
    sockets:     Resource[F, Socket[F]],
    sslOptions:  Option[SSLNegotiation.Options[F]],
    readTimeout: Duration
  ): Resource[F, MessageSocket[F]] =
    for
      bvs <- BitVectorSocket[F](sockets, sslOptions, readTimeout)
      ms  <- Resource.eval(fromBitVectorSocket(bvs, debug))
    yield ms
