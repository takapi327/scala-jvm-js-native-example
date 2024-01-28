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

import ldbc.connector.data.CommandId

case class ComQuery(sql: String) extends Message:

  override protected def encodeBody: Attempt[BitVector] =
    ComQuery.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_QUERY Request"

object ComQuery:

  val encoder: Encoder[ComQuery] = Encoder { comQuery =>

    val sqlBytes = comQuery.sql.getBytes("UTF-8")

    Attempt.successful(
      BitVector(CommandId.COM_QUERY) |+|
        BitVector(0x00) |+|
        BitVector(0x01) |+|
        BitVector(copyOf(sqlBytes, sqlBytes.length))
    )
  }
