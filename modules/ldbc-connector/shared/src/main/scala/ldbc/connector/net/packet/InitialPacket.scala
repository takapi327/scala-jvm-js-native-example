/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.codecs.*

import cats.syntax.all.*

import ldbc.connector.data.*

case class InitialPacket(
  protocolVersion: Int,
  serverVersion:   String,
  threadId:        Int,
  capabilityFlags: Seq[CapabilitiesFlags],
  scrambleBuff:    Array[Byte],
  authPlugin:      String
) extends Packet:

  override def toString: String = "InitialPacket"

object InitialPacket:

  private val protocolVersionCodec: Codec[Int] = uint8
  private val threadIdCodec:        Codec[Int] = int32
  private val authPluginDataPart1Codec: Codec[(Byte, Byte, Byte, Byte, Byte, Byte, Byte, Byte)] =
    byte :: byte :: byte :: byte :: byte :: byte :: byte :: byte
  private val capabilityFlagsLowerCodec: Codec[Int] = int16
  private val capabilityFlagsUpperCodec: Codec[Int] = int16

  val decoder: Decoder[InitialPacket] =
    for
      protocolVersion <- protocolVersionCodec.asDecoder
      serverVersion   <- nullTerminatedStringCodec.asDecoder
      threadId        <- threadIdCodec.asDecoder
      authPluginDataPart1 <- authPluginDataPart1Codec.map {
                               case (a, b, c, d, e, f, g, h) => Array(a, b, c, d, e, f, g, h)
                             }
      _                    <- ignore(8)     // Skip filter [0x00]
      capabilityFlagsLower <- capabilityFlagsLowerCodec.asDecoder
      _                    <- ignore(8 * 3) // Skip character set and status flags
      capabilityFlagsUpper <- capabilityFlagsUpperCodec.asDecoder
      capabilityFlags = (capabilityFlagsUpper << 16) | capabilityFlagsLower
      authPluginDataPart2Length <- if (capabilityFlags & (1 << 19)) != 0 then uint8.asDecoder else Decoder.pure(0)
      _                         <- ignore(10 * 8) // Skip reserved bytes (10 bytes)
      authPluginDataPart2       <- bytes(math.max(13, authPluginDataPart2Length - 8)).asDecoder
      authPluginName <-
        if (capabilityFlags & (1 << 19)) != 0 then nullTerminatedStringCodec.asDecoder else Decoder.pure("")
    yield
      val capabilityFlags = (capabilityFlagsUpper << 16) | capabilityFlagsLower
      InitialPacket(
        protocolVersion,
        serverVersion,
        threadId,
        CapabilitiesFlags(capabilityFlags),
        authPluginDataPart1 ++ authPluginDataPart2.toArray.dropRight(1),
        authPluginName
      )
