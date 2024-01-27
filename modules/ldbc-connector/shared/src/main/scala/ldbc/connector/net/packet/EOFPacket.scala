/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.codecs.*

case class EOFPacket(
  status:      Int,
  warnings:    Int,
  statusFlags: Int
) extends GenericResponsePackets:

  override def toString: String = s"EOF_Packet"

object EOFPacket:

  val STATUS = 0xfe

  val decoder: Decoder[EOFPacket] =
    for
      status      <- uint4
      warnings    <- uint4
      statusFlags <- uint4
    yield EOFPacket(status, warnings, statusFlags)
