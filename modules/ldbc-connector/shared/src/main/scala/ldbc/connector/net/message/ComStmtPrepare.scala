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

case class ComStmtPrepare(query: String) extends Message:

  override protected def encodeBody: Attempt[BitVector] =
    ComStmtPrepare.encoder.encode(this)

  override def encode: BitVector = encodeBody.require
  
  override def toString: String = "COM_STMT_PREPARE Request"

object ComStmtPrepare:

  val encoder: Encoder[ComStmtPrepare] = Encoder { comStmtPrepare =>
    val sqlBytes = comStmtPrepare.query.getBytes("UTF-8")
    Attempt.successful(
      BitVector(CommandId.COM_STMT_PREPARE) |+|
        BitVector(copyOf(sqlBytes, sqlBytes.length))
    )
  }
