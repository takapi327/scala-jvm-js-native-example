/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.codecs.*
import scodec.interop.cats.*

import cats.syntax.all.*

case class ResultSetRowPacket(value: List[Option[String]]) extends Packet:

  override def toString: String = s"ProtocolText::ResultSetRow"

object ResultSetRowPacket:

  def decodeValue(length: Int): Decoder[Option[String]] =
    bytes(length).asDecoder.map(_.decodeUtf8Lenient).map(value => if value.toUpperCase == "NULL" then None else value.some)

  def decoder(columns: Seq[ColumnDefinitionPacket]): Decoder[ResultSetRowPacket | EOFPacket] =
    uint8.flatMap {
      case EOFPacket.STATUS => EOFPacket.decoder
      case length => columns.zipWithIndex.toList.traverse((_, index) =>
        if index == 0 then decodeValue(length)
        else uint8.flatMap(length => decodeValue(length))
      ).map(ResultSetRowPacket(_))
    }
