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
case class ComStmtExecute(
  statementId: Long,
  numParams:   Int,
  params: Map[
    Int,
    None.type | Boolean | Byte | Short | Int | Long | Float | Double | BigDecimal | String | Array[Byte] |
      java.time.LocalTime | java.time.LocalDate | java.time.LocalDateTime
  ]
) extends Message:

  override protected def encodeBody: Attempt[BitVector] =
    ComStmtExecute.encoder.encode(this)

  override def encode: BitVector = encodeBody.require

  override def toString: String = "COM_STMT_EXECUTE Request"

object ComStmtExecute:

  private def zerofill(value: Int): Attempt[BitVector] = if value.toString.length == 1 then
    for
      fill <- uint32L.encode(0)
      int <- uint32L.encode(value)
    yield fill |+| int
  else uint32L.encode(value)

  val encoder: Encoder[ComStmtExecute] = Encoder { comStmtExecute =>

    val types = comStmtExecute.params.keys.foldLeft(BitVector.empty) { (acc, value) =>
      acc |+| BitVector(value) |+| BitVector(0) |+| BitVector(0)
    }

    val values = comStmtExecute.params.values.foldLeft(BitVector.empty) { (acc, value) =>
      acc |+| (value match
        case boolean: Boolean => uint8L.encode(if boolean then 1 else 0).require
        case byte: Byte       => uint8L.encode(byte).require
        case short: Short     => uint16L.encode(short).require
        case int: Int         => uint32L.encode(int).require
        case long: Long       => int64L.encode(long).require
        case f: Float     => float.encode(f).require
        case d: Double   => double.encode(d).require
        case bd: BigDecimal =>
          val bytes = bd.bigDecimal.unscaledValue.toByteArray
          BitVector(bytes.length) |+|
            BitVector(copyOf(bytes, bytes.length))
        case str: String =>
          val bytes = str.getBytes("UTF-8")
          BitVector(bytes.length) |+|
            BitVector(copyOf(bytes, bytes.length))
        case bytes: Array[Byte] =>
          BitVector(bytes.length) |+|
            BitVector(copyOf(bytes, bytes.length))
        case localTime: java.time.LocalTime =>
          val hour = localTime.getHour
          val minute = localTime.getMinute
          val second = localTime.getSecond
          val nano = localTime.getNano
          (hour, minute, second, nano) match
            case (0, 0, 0, 0) => BitVector(0)
            case (_, _, _, 0) =>
              (for
                length <- uint32L.encode(8)
                hour <- uint32L.encode(hour)
                minute <- uint32L.encode(minute)
                second <- uint32L.encode(second)
              yield length |+| hour |+| minute |+| second).require
            case _ =>
              (for
                length <- uint32L.encode(12)
                hour <- uint32L.encode(hour)
                minute <- uint32L.encode(minute)
                second <- uint32L.encode(second)
                nano <- uint32L.encode(nano)
              yield length |+| hour |+| minute |+| second |+| nano).require
        case localDate: java.time.LocalDate =>
          //val bytes = localDate.toString.getBytes("UTF-8")
          //  BitVector(bytes.length) |+|
          //    BitVector(copyOf(bytes, bytes.length))
          val year = localDate.getYear
          val month = localDate.getMonthValue
          val day = localDate.getDayOfMonth
          (year, month, day) match
            case (0, 0, 0) => BitVector(0)
            case _ =>
              (for
                length <- uint32L.encode(4)
                year <- uint32L.encode(year)
                month <- zerofill(month)
                day <- zerofill(day)
              yield length |+| year |+| month |+| day).require
        case localDateTime: java.time.LocalDateTime =>
          val bytes = localDateTime.toString.replace("T", " ").getBytes("UTF-8")
            BitVector(bytes.length) |+|
              BitVector(copyOf(bytes, bytes.length))
          /*
          val year = localDateTime.getYear
          val month = localDateTime.getMonthValue
          val day = localDateTime.getDayOfMonth
          val hour = localDateTime.getHour
          val minute = localDateTime.getMinute
          val second = localDateTime.getSecond
          val nano = localDateTime.getNano
          (year, month, day, hour, minute, second, nano) match
            case (0, 0, 0, 0, 0, 0, 0) => BitVector(0)
            case (_, _, _, 0, 0, 0, 0) =>
              (for
                length <- uint32L.encode(4)
                year <- uint32L.encode(year)
                month <- zerofill(month)
                day <- zerofill(day)
              yield length |+| year |+| month |+| day).require
            case (_, _, _, _, _, _, 0) =>
              (for
                length <- uint32L.encode(7)
                year <- uint32L.encode(year)
                month <- zerofill(month)
                day <- zerofill(day)
                hour <- uint32L.encode(hour)
                minute <- uint32L.encode(minute)
                second <- uint32L.encode(second)
              yield length |+| year |+| month |+| day |+| hour |+| minute |+| second).require
            case _ =>
              (for
                length <- uint32L.encode(11)
                year <- uint32L.encode(year)
                month <- zerofill(month)
                day <- zerofill(day)
                hour <- uint32L.encode(hour)
                minute <- uint32L.encode(minute)
                second <- uint32L.encode(second)
                nano <- uint32L.encode(nano)
              yield length |+| year |+| month |+| day |+| hour |+| minute |+| second |+| nano).require
           */
        case _          => throw new RuntimeException("Not implemented yet")
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
