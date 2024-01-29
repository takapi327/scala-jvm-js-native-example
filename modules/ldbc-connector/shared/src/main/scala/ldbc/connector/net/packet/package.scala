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
    
  def timestamp4: Decoder[java.time.LocalDateTime] =
    for
      year <- uint16L
      month <- uint8
      day <- uint8
    yield java.time.LocalDateTime.of(year, month, day, 0, 0, 0, 0)
  
  def timestamp7: Decoder[java.time.LocalDateTime] =
    for
      year <- uint16L
      month <- uint8
      day <- uint8
      hour        <- uint8
      minute      <- uint8L
      second      <- uint8L
    yield java.time.LocalDateTime.of(year, month, day, hour, minute, second, 0)

  def timestamp11: Decoder[java.time.LocalDateTime] =
    for
      year <- uint16L
      month <- uint8
      day <- uint8
      hour <- uint8
      minute <- uint8L
      second <- uint8L
      microsecond <- uint32L
    yield java.time.LocalDateTime.of(year, month, day, hour, minute, second, microsecond.toInt * 1000)


  /**
   * 
   * <table>
   *   <thead>
   *     <tr>
   *       <th left>Type</th>
   *       <th>Name</th>
   *       <th>Description</th>
   *     </tr>
   *   </thead>
   *   <tbody>
   *     <tr>
   *       <td>int<1></td>
   *       <td>length</td>
   *       <td>number of bytes following (valid values: 0, 8, 12)</td>
   *     </tr>
   *     <tr>
   *       <td>int<1></td>
   *       <td>is_negative</td>
   *       <td>1 if minus, 0 for plus</td>
   *     </tr>
   *     <tr>
   *       <td>int<4></td>
   *       <td>days</td>
   *       <td>days</td>
   *     </tr>
   *     <tr>
   *       <td>int<1></td>
   *       <td>hour</td>
   *       <td>hour</td>
   *     </tr>
   *     <tr>
   *       <td>int<1></td>
   *       <td>minute</td>
   *       <td>minute</td>
   *     </tr>
   *     <tr>
   *       <td>int<1></td>
   *       <td>second</td>
   *       <td>second</td>
   *     </tr>
   *     <tr>
   *       <td>int<1></td>
   *       <td>microsecond</td>
   *       <td>micro seconds</td>
   *     </tr>
   *   </tbody>
   * </table>
   */
  def timestamp: Decoder[Option[java.time.LocalDateTime]] =
    uint8.flatMap {
      case 0 => Decoder.pure(None)
      case 4 => timestamp4.map(Some(_))
      case 7 => timestamp7.map(Some(_))
      case 11 => timestamp11.map(Some(_))
      case _ => throw new IllegalArgumentException("Invalid timestamp length")
    }

  def date: Decoder[java.time.LocalDate] =
    for
      year  <- uint16L
      month <- uint8
      day   <- uint8
    yield java.time.LocalDate.of(year, month, day)
