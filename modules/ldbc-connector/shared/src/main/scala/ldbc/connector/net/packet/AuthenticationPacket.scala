/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scala.annotation.switch

import scodec.*
import scodec.codecs.*

trait AuthenticationPacket extends Packet

object AuthenticationPacket:

  val decoder: Decoder[AuthenticationPacket] =
    int8.flatMap { status =>
      (status: @switch) match
        case AuthMoreDataPacket.STATUS => AuthMoreDataPacket.decoder
        case OKPacket.STATUS           => OKPacket.decoder
    }
