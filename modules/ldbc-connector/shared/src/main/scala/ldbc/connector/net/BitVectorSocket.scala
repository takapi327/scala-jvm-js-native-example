/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

import cats.*
import cats.syntax.all.*

import cats.effect.*
import cats.effect.syntax.temporal.*

import fs2.Chunk
import fs2.io.net.Socket

import scodec.bits.BitVector

import ldbc.connector.exception.EofException
import ldbc.connector.net.packet.InitialPacket

/**
 *  A higher-level `Socket` interface defined in terms of `BitVector`.
 */
trait BitVectorSocket[F[_]]:

  def initialPacket: InitialPacket

  /** Write the specified bits to the socket. */
  def write(bits: BitVector): F[Unit]

  /**
   * Read `nBytes` bytes (not bits!) from the socket, or fail with an exception if EOF is reached
   * before `nBytes` bytes are received.
   */
  def read(nBytes: Int): F[BitVector]

object BitVectorSocket:

  private def parseHeader(chunk: Chunk[Byte]): Int =
    val headerBytes = chunk.toArray
    (headerBytes(0) & 0xFF) | ((headerBytes(1) & 0xFF) << 8) | ((headerBytes(2) & 0xFF) << 16)

  def readInitialPacket[F[_]: Temporal](socket: Socket[F])(using ev: ApplicativeError[F, Throwable]): F[InitialPacket] =
    for
      header <- socket.read(4).flatMap {
        case Some(chunk) => Monad[F].pure(chunk)
        case None => ev.raiseError(new Exception("Failed to read header"))
      }
      payloadSize = parseHeader(header)
      payload <- socket.read(payloadSize).flatMap {
        case Some(chunk) => Monad[F].pure(chunk)
        case None => ev.raiseError(new Exception("Failed to read payload"))
      }
      initialPacket <- InitialPacket.decoder.decode(payload.toBitVector).fold(
        err => ev.raiseError[InitialPacket](new Exception(s"Failed to decode initial packet: $err ${payload.toBitVector.toHex}")),
        result => Monad[F].pure(result.value)
      )
    yield initialPacket

  /**
   * Construct a `BitVectorSocket` by wrapping an existing `Socket`.
   *
   * @param socket the underlying `Socket`
   * @group Constructors
   */
  def fromSocket[F[_]](
    socket: Socket[F],
    initPacket: InitialPacket,
    readTimeout: Duration,
    carryRef: Ref[F, Chunk[Byte]],
    useSSL: Boolean
  )(using F: Temporal[F]): BitVectorSocket[F] =
    new BitVectorSocket[F]:

      private var sequenceId: Byte = if useSSL then 2 else 1

      override def initialPacket: InitialPacket = initPacket

      private val withTimeout: F[Option[Chunk[Byte]]] => F[Option[Chunk[Byte]]] = readTimeout match
        case _: Duration.Infinite => identity
        case finite: FiniteDuration => _.timeout(finite)

      private def readUntilN(nBytes: Int, carry: Chunk[Byte]): F[BitVector] =
        if carry.size < nBytes then
          withTimeout(socket.read(8192)).flatMap {
            case Some(bytes) => readUntilN(nBytes, carry ++ bytes)
            case None => F.raiseError(EofException(nBytes, carry.size))
          }
        else
          val (output, remainder) = carry.splitAt(nBytes)
          carryRef.set(remainder).as(output.toBitVector)

      override def write(bits: BitVector): F[Unit] =
        val payloadSize = bits.toByteArray.length
        val header = Chunk(
          payloadSize.toByte,
          ((payloadSize >> 8) & 0xFF).toByte,
          ((payloadSize >> 16) & 0xFF).toByte,
          sequenceId
        )
        sequenceId = ((sequenceId + 1) % 256).toByte
        socket.write(header ++ Chunk.byteVector(bits.bytes))

      override def read(nBytes: Int): F[BitVector] =
        // nb: unsafe for concurrent reads but protected by protocol mutex
        carryRef.get.flatMap(carry => readUntilN(nBytes, carry))

  def apply[F[_]: Temporal](
    socket:      Resource[F, Socket[F]],
    sslOptions:  Option[SSLNegotiation.Options[F]],
    readTimeout: Duration
  ): Resource[F, BitVectorSocket[F]] =
    for
      socket <- socket
      initialPacket <- Resource.eval(readInitialPacket(socket))
      socket$ <- sslOptions.fold(socket.pure[Resource[F, *]])(SSLNegotiation.negotiateSSL(socket, _))
      carryRef <- Resource.eval(Ref[F].of(Chunk.empty[Byte]))
    yield fromSocket(socket$, initialPacket, readTimeout, carryRef, sslOptions.isDefined)
