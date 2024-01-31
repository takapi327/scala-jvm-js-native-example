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
          case (ColumnDataType.MYSQL_TYPE_TINY, value: Byte) => acc ++ uint8L.encode(value).require
          case (ColumnDataType.MYSQL_TYPE_VARCHAR, value: String) =>
            val bytes = value.getBytes("UTF-8")
            acc ++ BitVector(copyOf(bytes, bytes.length))
          case (_, unknown) => throw new IllegalArgumentException(s"Unsupported data type: $unknown")
    })
  }
