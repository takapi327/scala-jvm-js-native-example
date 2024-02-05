/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.message

import cats.syntax.all.*

import scodec.*
import scodec.bits.BitVector
import scodec.interop.cats.*

import ldbc.connector.data.CommandId

case class ComStmtClose(statementId: Long) extends Message:

  override protected def encodeBody: Attempt[BitVector] =
    ComStmtClose.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_STMT_CLOSE Request"

object ComStmtClose:
  
  val encoder: Encoder[ComStmtClose] = Encoder(comStmtClose => 
    Attempt.Successful(
      BitVector(CommandId.COM_STMT_CLOSE) |+|
        BitVector(comStmtClose.statementId) |+|
        BitVector(Array[Byte](0, 0, 0))
    )
  )
