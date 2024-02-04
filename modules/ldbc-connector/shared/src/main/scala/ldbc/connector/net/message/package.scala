/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import cats.syntax.all.*

import scodec.bits.BitVector
import scodec.interop.cats.*

import ldbc.connector.data.ColumnDataType

package object message:

  def nullBitmap(columns: List[ColumnDataType]): BitVector =
    val count = columns.length
    if count > 0 then
      // val size = (count + 7) / 8
      //
      // @annotation.tailrec
      // def buildBitVector(index: Int, acc: BitVector): BitVector =
      //  if index == size then acc
      //  else buildBitVector(index + 1, acc |+| BitVector(0))

      //BitVector(count) |+| buildBitVector(0, BitVector.empty)
      BitVector(count) |+| BitVector(columns.map {
       case ColumnDataType.MYSQL_TYPE_NULL => 1
       case _ => 1
      }.sum)
      //BitVector(count) |+| BitVector(0)
    else BitVector.empty
