/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.codecs.*

case class ERRPacket(
  status:         Int,
  errorCode:      Int,
  sqlStateMarker: Int,
  sqlState:       String,
  errorMessage:   String
) extends GenericResponsePackets:
  
  override def toString: String = "ERR_Packet"

object ERRPacket:

  val STATUS = 0xff

  val decoder: Decoder[ERRPacket] =
    for
      errorCode      <- uint16L
      sqlStateMarker <- uint8
      sqlState       <- bytes(5)
      errorMessage   <- bytes
    yield ERRPacket(
      STATUS,
      errorCode,
      sqlStateMarker,
      sqlState.decodeUtf8Lenient,
      errorMessage.decodeUtf8Lenient
    )
