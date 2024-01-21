/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import fs2.Chunk

import scodec.Decoder
import scodec.bits.BitVector

case class ResponsePacket(
  header: BitVector,
  payload: BitVector
) extends Packet:
  
  private val payloadBytes: Chunk[Byte] = Chunk.array(payload.toByteArray)

  def isErrorPacket: Boolean = (payloadBytes(0) & 0xff) == ResponsePacket.TYPE_ID_ERROR

  def isEOFPacket: Boolean = ((payloadBytes(0) & 0xff) == ResponsePacket.TYPE_ID_EOF) && (payload.size <= 5)

  def isAuthMethodSwitchRequestPacket: Boolean = (payloadBytes(0) & 0xff) == ResponsePacket.TYPE_ID_AUTH_SWITCH

  def isOKPacket: Boolean = (payloadBytes(0) & 0xff) == ResponsePacket.TYPE_ID_OK

  def isAuthMoreDataPacket: Boolean = (payloadBytes(0) & 0xff) == ResponsePacket.TYPE_ID_AUTH_MORE_DATA

  def isAuthNextFactorPacket: Boolean = (payloadBytes(0) & 0xff) == ResponsePacket.TYPE_ID_AUTH_NEXT_FACTOR

  def to[M](decoder: Decoder[M]): M = decoder.decode(payload).require.value

object ResponsePacket:
  val TYPE_ID_ERROR: Int = 0xFF
  val TYPE_ID_EOF: Int = 0xFE
  val TYPE_ID_AUTH_SWITCH: Int = 0xFE
  val TYPE_ID_LOCAL_INFILE: Int = 0xFB
  val TYPE_ID_OK: Int = 0
  val TYPE_ID_AUTH_MORE_DATA: Int = 0x01
  val TYPE_ID_AUTH_NEXT_FACTOR: Int = 0x02
