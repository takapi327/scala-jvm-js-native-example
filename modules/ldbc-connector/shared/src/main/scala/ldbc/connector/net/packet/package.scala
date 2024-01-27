/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scodec.*
import scodec.codecs.*
import scodec.bits.BitVector

package object packet:

  def nullTerminatedStringCodec: Codec[String] = new Codec[String]:
    def sizeBound: SizeBound = SizeBound.unknown

    def encode(value: String): Attempt[BitVector] =
      Attempt.successful(BitVector(value.getBytes(java.nio.charset.StandardCharsets.UTF_8) :+ 0.toByte))

    def decode(bits: BitVector): Attempt[DecodeResult[String]] =
      val bytes     = bits.bytes.takeWhile(_ != 0)
      val string    = new String(bytes.toArray, java.nio.charset.StandardCharsets.UTF_8)
      val remainder = bits.drop((bytes.size + 1) * 8) // +1 is a null character, so *8 is a byte to bit
      Attempt.successful(DecodeResult(string, remainder))

  def time: Decoder[java.time.LocalTime] =
    for
      hour        <- uint8
      minute      <- uint8
      second      <- uint8
      microsecond <- uint32L
    yield java.time.LocalTime.of(hour, minute, second, microsecond.toInt * 1000)

  def timestamp(length: Int): Decoder[java.time.LocalDateTime] =
    bytes(length).asDecoder.map(_.decodeUtf8 match
      case Left(value) => throw value
      case Right(value) =>
        java.time.LocalDateTime.parse(value, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    )

  def date: Decoder[java.time.LocalDate] =
    for
      year  <- uint16L
      month <- uint8
      day   <- uint8
    yield java.time.LocalDate.of(year, month, day)
