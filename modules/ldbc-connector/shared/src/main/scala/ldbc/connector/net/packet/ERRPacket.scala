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
  sqlStateMarker: String,
  sqlState:       String,
  errorMessage:   String
) extends GenericResponsePackets

object ERRPacket:

  val STATUS = 0xff

  val decoder: Decoder[ERRPacket] =
    for
      status         <- uint4
      errorCode      <- uint2
      sqlStateMarker <- bytes(1)
      sqlState       <- bytes(5)
      errorMessage   <- variableSizeBytes(uint8, utf8)
    yield ERRPacket(
      status,
      errorCode,
      sqlStateMarker.decodeUtf8.toOption.get,
      sqlState.decodeUtf8.toOption.get,
      errorMessage
    )
