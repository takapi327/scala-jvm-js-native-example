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

import ldbc.connector.data.ColumnDataType

case class BinaryProtocolResultSetRowPacket(value: List[Option[String]]) extends Packet:

  override def toString: String = "Binary Protocol Resultset Row"

object BinaryProtocolResultSetRowPacket:

  private def addLeadingZeroIfNeeded(input: Int): String = {
    if (input.toString.length == 1) "0" + input
    else input.toString
  }

  def decodeValue(column: ColumnDefinitionPacket): Decoder[Option[String]] =
    column.columnType match
      case ColumnDataType.MYSQL_TYPE_LONGLONG => int64L.asDecoder.map(_.toString.some)
      case ColumnDataType.MYSQL_TYPE_TINY => uint8L.asDecoder.map(_.toString.some)
      case ColumnDataType.MYSQL_TYPE_VARCHAR => uint8L.flatMap(bytes(_)).asDecoder.map(_.decodeUtf8Lenient.some)
      case ColumnDataType.MYSQL_TYPE_TIMESTAMP => timestamp.asDecoder.map(_.map(timestamp => s"${timestamp.getYear}-${addLeadingZeroIfNeeded(timestamp.getMonthValue)}-${timestamp.getDayOfMonth} ${addLeadingZeroIfNeeded(timestamp.getHour)}:${addLeadingZeroIfNeeded(timestamp.getMinute)}:${addLeadingZeroIfNeeded(timestamp.getSecond)}.${timestamp.getNano}"))
      case _ => uint8L.flatMap(bytes(_)).asDecoder.map(_.decodeUtf8Lenient.some)
  
  def decoder(columns: List[ColumnDefinitionPacket]): Decoder[BinaryProtocolResultSetRowPacket | EOFPacket | ERRPacket] =
    uint8.flatMap {
      case EOFPacket.STATUS => EOFPacket.decoder
      case ERRPacket.STATUS => ERRPacket.decoder
      case _ =>
        for
          nullBitmap <- bytes((columns.length + 7 + 2) / 8).asDecoder
          values <- columns.traverse(decodeValue)
        yield BinaryProtocolResultSetRowPacket(values)
    }
