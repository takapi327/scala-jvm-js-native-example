/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

//import cats.syntax.all.*

import scodec.bits.BitVector
import scodec.codecs.*
//import scodec.interop.cats.*

import ldbc.connector.data.ColumnDataType

package object message:

  def nullBitmap(columns: List[ColumnDataType]): BitVector =
    if columns.nonEmpty then
      //val size = (columns.length + 7) / 8
      //
      // @annotation.tailrec
      // def buildBitVector(index: Int, acc: BitVector): BitVector =
      //  if index == size then acc
      //  else buildBitVector(index + 1, acc |+| BitVector(0))

      //buildBitVector(0, BitVector.empty)
      if columns.contains(ColumnDataType.MYSQL_TYPE_NULL) then
        val bitmap = columns.reverse.foldLeft(0) { (bitmap, param) =>
          param match
            case ColumnDataType.MYSQL_TYPE_NULL => (bitmap << 1) | 1
            case _                              => (bitmap << 1) | 0
        }
        println(s"bitmap: $bitmap")
        uint8.encode(bitmap).require
      else BitVector(0)
    else BitVector.empty
