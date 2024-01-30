/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.message

import scodec.*
import scodec.bits.BitVector

import ldbc.connector.data.CommandId

case class ComQuit() extends Message:

  override protected def encodeBody: Attempt[BitVector] =
    ComQuit.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_QUIT Request"

object ComQuit:

  val encoder: Encoder[ComQuit] = Encoder(_ => Attempt.successful(BitVector(CommandId.COM_QUIT)))
