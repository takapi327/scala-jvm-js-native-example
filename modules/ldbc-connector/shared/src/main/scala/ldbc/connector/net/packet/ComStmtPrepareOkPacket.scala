/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scala.annotation.switch

import scodec.*
import scodec.codecs.*

case class ComStmtPrepareOkPacket(
  status:          Int,
  statementId:     Long,
  numColumns:      Int,
  numParams:       Int,
  reserved1:       Int,
  warningCount:    Int,
  metadataFollows: Int
) extends Packet:

  override def toString: String = "COM_STMT_PREPARE_OK Packet"

object ComStmtPrepareOkPacket:

  val decoder: Decoder[ComStmtPrepareOkPacket | ERRPacket] =
    uint8.flatMap { status =>
      (status: @switch) match
        case ERRPacket.STATUS => ERRPacket.decoder
        case OKPacket.STATUS =>
          for
            statementId  <- uint32L.asDecoder
            numColumns   <- uint16L.asDecoder
            numParams    <- uint16L.asDecoder
            reserved1    <- uint8L.asDecoder
            warningCount <- uint16L.asDecoder
          // metadataFollows <- uint8.asDecoder
          yield ComStmtPrepareOkPacket(status, statementId, numColumns, numParams, reserved1, warningCount, 0)
    }
