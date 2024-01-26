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

import ldbc.connector.util.DataType

case class ResultSetRowPacket(value: Seq[String | java.time.LocalTime | java.time.LocalDateTime | java.time.LocalDate | None.type]) extends Packet:
  override def toString: String = s"ProtocolText::ResultSetRow"

object ResultSetRowPacket:

  def decoder(columns: Seq[ColumnDefinitionPacket]): Decoder[ResultSetRowPacket] =
    columns.traverse(column => uint8.flatMap(length =>
      if length == 0xfe then Decoder.pure(None)
      else column.columnType match
        case DataType.MYSQL_TYPE_TIMESTAMP => timestamp(length).asDecoder
        case DataType.MYSQL_TYPE_DATE => date.asDecoder
        case DataType.MYSQL_TYPE_TIME => time.asDecoder
        case _ => bytes(length).asDecoder.map(_.decodeUtf8.getOrElse(""))
    )).map(ResultSetRowPacket(_))
