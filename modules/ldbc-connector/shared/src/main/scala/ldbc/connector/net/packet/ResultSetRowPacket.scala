/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.codecs.*

import cats.syntax.all.*

case class ResultSetRowPacket(value: Seq[String]) extends Packet:
  override def toString: String = s"ProtocolText::ResultSetRow"

object ResultSetRowPacket:

  def decoder(columnLength: Int): Decoder[ResultSetRowPacket] =
    def read(remaining: Int, acc: List[String]): Decoder[ResultSetRowPacket] =
      if remaining <= 0 then Decoder.pure(ResultSetRowPacket(acc))
      else
        uint8.flatMap(length =>
          if length == 0xfe then read(remaining - 1, acc)
          else
            bytes(length).asDecoder.map(_.decodeUtf8.getOrElse("")).flatMap(value => read(remaining - 1, acc :+ value))
        )

    read(columnLength, List.empty)
