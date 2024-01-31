/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net

import cats.syntax.all.*

import scodec.bits.BitVector
import scodec.interop.cats.*

package object message:

  def nullBitmap(count: Int): BitVector = if count > 0 then
    val size = (count + 7) / 8

    @annotation.tailrec
    def buildBitVector(index: Int, acc: BitVector): BitVector =
      if index == size then acc
      else buildBitVector(index + 1, acc |+| BitVector(0))

    BitVector(count) |+| buildBitVector(0, BitVector.empty)
  else BitVector.empty
