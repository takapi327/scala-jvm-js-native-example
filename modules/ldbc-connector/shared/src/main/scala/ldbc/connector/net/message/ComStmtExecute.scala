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
import scodec.codecs.*
import scodec.interop.cats.*

import ldbc.connector.data.*

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_stmt_execute.html
case class ComStmtExecute(statementId: Long, numParams: Int, params: Map[Int, Long | String]) extends Message:

  override protected def encodeBody: Attempt[BitVector] =
    ComStmtExecute.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_STMT_EXECUTE Request"

object ComStmtExecute:

  val encoder: Encoder[ComStmtExecute] = Encoder { comStmtExecute =>

    val types = comStmtExecute.params.keys.foldLeft(BitVector.empty) { (acc, value) =>
      acc |+| BitVector(value) |+| BitVector(0) |+| BitVector(0)
    }

    val values = comStmtExecute.params.values.foldLeft(BitVector.empty) { (acc, value) =>
      acc |+| (value match
        case str: String =>
          val bytes = str.getBytes("UTF-8")
          BitVector(bytes.length) |+|
            BitVector(copyOf(bytes, bytes.length))
        case long: Long => int64L.encode(long).require
      )
    }

    Attempt.successful(
      BitVector(CommandId.COM_STMT_EXECUTE) |+|
        BitVector(comStmtExecute.statementId) |+|
        BitVector(Array[Byte](0, 0, 0)) |+|
        BitVector(EnumCursorType.PARAMETER_COUNT_AVAILABLE.code) |+|
        BitVector(Array[Byte](1, 0, 0, 0)) |+|
        nullBitmap(comStmtExecute.numParams) |+|
        BitVector(1) |+| // new_params_bind_flag,	Always 1. Malformed packet error if not 1
        types |+|
        values
    )
  }

  // @see https://dev.mysql.com/doc/dev/mysql-server/latest/mysql__com_8h.html
  enum EnumCursorType(val code: Short):
    case CURSOR_TYPE_NO_CURSOR     extends EnumCursorType(0)
    case CURSOR_TYPE_READ_ONLY     extends EnumCursorType(1)
    case CURSOR_TYPE_FOR_UPDATE    extends EnumCursorType(2)
    case CURSOR_TYPE_SCROLLABLE    extends EnumCursorType(4)
    case PARAMETER_COUNT_AVAILABLE extends EnumCursorType(8)
