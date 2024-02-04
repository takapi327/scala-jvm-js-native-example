/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import scodec.bits.BitVector
import scodec.codecs.*

import ldbc.connector.data.ColumnDataType

package object message:

  def nullBitmap(columns: List[ColumnDataType]): BitVector =
    if columns.nonEmpty then
      if columns.contains(ColumnDataType.MYSQL_TYPE_NULL) then
        val bitmap = columns.reverse.foldLeft(0) { (bitmap, param) =>
          param match
            case ColumnDataType.MYSQL_TYPE_NULL => (bitmap << 1) | 1
            case _                              => (bitmap << 1) | 0
        }
        uint8.encode(bitmap).require
      else BitVector(0)
    else BitVector.empty
