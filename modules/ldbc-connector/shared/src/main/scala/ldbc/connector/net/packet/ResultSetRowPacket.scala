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

import ldbc.connector.data.*

case class ResultSetRowPacket(value: Seq[Column[Byte | Int | Short | Long | String | java.time.LocalTime | java.time.LocalDate | java.time.LocalDateTime]]) extends Packet:
  override def toString: String = s"ProtocolText::ResultSetRow"

object ResultSetRowPacket:
  
  private def columnDecode(column: ColumnDefinitionPacket, length: Int): Decoder[Column[Byte | Int | Short | Long | String | java.time.LocalTime | java.time.LocalDate | java.time.LocalDateTime]] =
    column.columnType match
      case ColumnDataType.MYSQL_TYPE_TIMESTAMP => timestamp(length).asDecoder.map(value => Column[java.time.LocalDateTime](column, value))
      case ColumnDataType.MYSQL_TYPE_DATE => date.asDecoder.map(value => Column[java.time.LocalDate](column, value))
      case ColumnDataType.MYSQL_TYPE_TIME => time.asDecoder.map(value => Column[java.time.LocalTime](column, value))
      case ColumnDataType.MYSQL_TYPE_TINY if column.flags.contains(ColumnDefinitionFlags.UNSIGNED_FLAG) => uint8.asDecoder.map(value => Column[Short](column, value.toShort))
      case ColumnDataType.MYSQL_TYPE_TINY => bytes(length).map(_.decodeUtf8Lenient.toShort).asDecoder.map(value => Column[Short](column, value))
      case ColumnDataType.MYSQL_TYPE_LONGLONG if column.flags.contains(ColumnDefinitionFlags.UNSIGNED_FLAG) =>
        bytes(length).map(_.decodeUtf8Lenient.toLong).asDecoder.map(value => Column[Long](column, value))
      case ColumnDataType.MYSQL_TYPE_LONGLONG => int64L.asDecoder.map(value => Column[Long](column, value))
      case _ => bytes(length).asDecoder.map(_.decodeUtf8Lenient).map(value => Column[String](column, value))

  def decoder(columns: Seq[ColumnDefinitionPacket]): Decoder[ResultSetRowPacket | EOFPacket] =
    uint8.flatMap {
      case EOFPacket.STATUS => EOFPacket.decoder
      case length => columns.zipWithIndex.traverse((column, index) =>
        if index == 0 then columnDecode(column, length)
        else uint8.flatMap(length => columnDecode(column, length))
      ).map(ResultSetRowPacket(_))
    }
