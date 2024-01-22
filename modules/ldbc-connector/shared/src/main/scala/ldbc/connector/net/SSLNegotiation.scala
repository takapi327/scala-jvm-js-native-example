/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import cats.*
import cats.effect.*
import cats.syntax.all.*

import fs2.Chunk
import fs2.io.net.*
import fs2.io.net.tls.*

object SSLNegotiation:

  /** Parameters for `negotiateSSL`. */
  case class Options[F[_]](
    tlsContext:    TLSContext[F],
    tlsParameters: TLSParameters,
    fallbackOk:    Boolean,
    logger:        Option[String => F[Unit]]
  )

  private val SSLRequest: Chunk[Byte] =
    val array = Array[Byte](
      32.toByte,
      0,
      0,
      1,
      0x07,
      0xaa.toByte,
      0x3e,
      0x19,
      0xff.toByte,
      0xff.toByte,
      0xff.toByte,
      0x0,
      0xff.toByte
    ) ++ new Array[Byte](23)

    Chunk.array(array)

  def negotiateSSL[F[_]](
    socket:     Socket[F],
    sslOptions: SSLNegotiation.Options[F]
  ): Resource[F, Socket[F]] =
    Resource.eval(socket.write(SSLRequest)) *>
      sslOptions.tlsContext
        .clientBuilder(socket)
        .withParameters(sslOptions.tlsParameters)
        .withLogger(
          sslOptions.logger.fold[TLSLogger[F]](TLSLogger.Disabled)(logger => TLSLogger.Enabled(x => logger(x)))
        )
        .build
