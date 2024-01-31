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
import scodec.interop.cats.*

import ldbc.connector.data.*

case class ComQuery(sql: String, capabilityFlags: Seq[CapabilitiesFlags], params: Map[ColumnDataType, Any]) extends Message:

  override protected def encodeBody: Attempt[BitVector] =
    ComQuery.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_QUERY Request"

object ComQuery:

  val encoder: Encoder[ComQuery] = Encoder { comQuery =>

    val sqlBytes = comQuery.sql.getBytes("UTF-8")

    val hasQueryAttributes = comQuery.capabilityFlags.contains(CapabilitiesFlags.CLIENT_QUERY_ATTRIBUTES)

    val parameterCount = comQuery.params.size

    val parameter = if hasQueryAttributes then
      BitVector(comQuery.params.size) |+| BitVector(0x01)
    else BitVector.empty

    val nullBitmaps = if hasQueryAttributes && parameterCount > 0 then

      val names = comQuery.params.map { (columnType, param) =>
        val bytes = param.toString.getBytes("UTF-8")
        BitVector(columnType.code) |+| BitVector(0x00) |+| BitVector(copyOf(bytes, bytes.length))
      }.toList.combineAll

      nullBitmap(parameterCount) |+|
        BitVector(0x01) |+|
        names |+| 
        BinaryProtocolValue(comQuery.params).encode
    else BitVector.empty

    Attempt.successful(
      BitVector(CommandId.COM_QUERY) |+|
        parameter |+|
        nullBitmaps |+|
        BitVector(copyOf(sqlBytes, sqlBytes.length))
    )
  }
