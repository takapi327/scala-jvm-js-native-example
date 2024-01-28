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

import ldbc.connector.data.*

// https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_com_stmt_execute.html
case class ComStmtExecute(statementId: Long, numParams: Int, params: List[String]) extends Message:

  override protected def encodeBody: Attempt[BitVector] =
    ComStmtExecute.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_STMT_EXECUTE Request"

object ComStmtExecute:

  val encoder: Encoder[ComStmtExecute] = Encoder { comStmtExecute =>
    val hoge = if comStmtExecute.numParams > 0 then
      val size = (comStmtExecute.numParams + 7) / 8
      var test: BitVector = BitVector.empty
      for i <- 0 until size do test = test |+| BitVector(0)
      test
    else BitVector.empty

    var values = BitVector.empty
    comStmtExecute.params.foreach(value =>
      val bytes = value.getBytes("UTF-8")
      values = values |+| BitVector(ColumnDataType.MYSQL_TYPE_VARCHAR.code) |+| BitVector(0) |+| BitVector(
        value.length
      ) |+| BitVector(copyOf(bytes, bytes.length))
    )

    Attempt.successful(
      BitVector(CommandId.COM_STMT_EXECUTE) |+|
        BitVector(comStmtExecute.statementId) |+|
        BitVector(0) |+|
        BitVector(0) |+|
        BitVector(0) |+|
        BitVector(EnumCursorType.CURSOR_TYPE_NO_CURSOR.code) |+|
        BitVector(1) |+|
        BitVector(0) |+|
        BitVector(0) |+|
        BitVector(0) |+|
        // BitVector(0) |+|
        hoge |+|
        // BitVector(comStmtExecute.params.length.toString.length) |+|
        // BitVector(comStmtExecute.params.length) |+|
        // BitVector(0x0f) |+|
        // BitVector(0) |+|
        // BitVector(1) |+|
        // BitVector(ColumnDataType.MYSQL_TYPE_VARCHAR.code) |+|
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
