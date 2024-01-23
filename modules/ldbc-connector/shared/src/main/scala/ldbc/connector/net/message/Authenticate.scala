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

case class Authenticate(user: String, hashedPassword: Array[Byte], pluginName: String) extends Message:

  override protected def encodeBody: Attempt[BitVector] = Authenticate.encoder.encode(this)

  override def encode: BitVector =
    encodeBody.require

  override def toString: String = "Authenticate Request"

object Authenticate:

  val encoder: Encoder[Authenticate] = Encoder { auth =>
    val capabilityFlags = hex"07a23e19".bits
    val maxPacketSize   = hex"ffffff00".bits
    val characterSet    = hex"ff".bits
    val userBytes       = auth.user.getBytes("UTF-8")

    val reserved = BitVector.fill(23 * 8)(false) // 23 bytes of zero

    val pluginBytes = auth.pluginName.getBytes("UTF-8")

    Attempt.successful(
      capabilityFlags |+|
        maxPacketSize |+|
        characterSet |+|
        reserved |+|
        BitVector(copyOf(userBytes, userBytes.length + 1)) |+|
        BitVector(copyOf(auth.hashedPassword, auth.hashedPassword.length)) |+|
        BitVector(copyOf(pluginBytes, pluginBytes.length + 2))
    )
  }
