/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.message

import java.util.Arrays.copyOf

import cats.syntax.all.*

import scodec.*
import scodec.bits.*
import scodec.codecs.*

import ldbc.connector.data.*

case class BinaryProtocolValue(values: Map[ColumnDataType, Any]) extends Message:

  override protected def encodeBody: Attempt[BitVector] =
    BinaryProtocolValue.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

object BinaryProtocolValue:

  val encoder: Encoder[BinaryProtocolValue] = Encoder { binaryProtocolValue =>
    Attempt.successful(binaryProtocolValue.values.foldLeft(BitVector.empty) {
      case (acc, tuple) =>
        tuple match
          case (ColumnDataType.MYSQL_TYPE_NULL, _)              => acc
          case (ColumnDataType.MYSQL_TYPE_TINY, value: Boolean) => acc ++ uint8L.encode(if value then 1 else 0).require
          case (ColumnDataType.MYSQL_TYPE_TINY, value: Byte)    => acc ++ uint8L.encode(value).require
          case (ColumnDataType.MYSQL_TYPE_SHORT | ColumnDataType.MYSQL_TYPE_YEAR, value: Short) =>
            acc ++ uint16L.encode(value).require
          case (ColumnDataType.MYSQL_TYPE_LONG | ColumnDataType.MYSQL_TYPE_INT24, value: Int) =>
            acc ++ uint32L.encode(value).require
          case (ColumnDataType.MYSQL_TYPE_LONGLONG, value: Long)         => acc ++ int64L.encode(value).require
          case (ColumnDataType.MYSQL_TYPE_FLOAT, value: Float)           => acc ++ float.encode(value).require
          case (ColumnDataType.MYSQL_TYPE_DOUBLE, value: Double)         => acc ++ double.encode(value).require
          case (ColumnDataType.MYSQL_TYPE_NEWDECIMAL, value: BigDecimal) => ???
          case (
              ColumnDataType.MYSQL_TYPE_STRING | ColumnDataType.MYSQL_TYPE_VARCHAR | ColumnDataType.MYSQL_TYPE_ENUM |
              ColumnDataType.MYSQL_TYPE_SET | ColumnDataType.MYSQL_TYPE_LONG_BLOB |
              ColumnDataType.MYSQL_TYPE_MEDIUM_BLOB | ColumnDataType.MYSQL_TYPE_BLOB |
              ColumnDataType.MYSQL_TYPE_TINY_BLOB | ColumnDataType.MYSQL_TYPE_GEOMETRY |
              ColumnDataType.MYSQL_TYPE_BIT | ColumnDataType.MYSQL_TYPE_DECIMAL | ColumnDataType.MYSQL_TYPE_NEWDECIMAL,
              value: String
            ) =>
            val bytes = value.getBytes("UTF-8")
            acc ++ BitVector(copyOf(bytes, bytes.length))
          case (ColumnDataType.MYSQL_TYPE_VAR_STRING, value: Array[Byte]) =>
            acc ++ BitVector(copyOf(value, value.length))
          case (ColumnDataType.MYSQL_TYPE_DATE, value: java.time.LocalDate)          => ???
          case (ColumnDataType.MYSQL_TYPE_DATETIME, value: java.time.LocalDateTime)  => ???
          case (ColumnDataType.MYSQL_TYPE_TIMESTAMP, value: java.time.LocalDateTime) => ???
          case (_, unknown) => throw new IllegalArgumentException(s"Unsupported data type: $unknown")
    })
  }
