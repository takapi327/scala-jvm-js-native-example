/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.codecs.*

case class ColumnsNumberPacket(
  columnCount: Int
) extends Packet:

  override def toString: String = "ColumnsNumber Packet"

object ColumnsNumberPacket:

  val decoder: Decoder[ColumnsNumberPacket] = uint8.map(ColumnsNumberPacket(_))
