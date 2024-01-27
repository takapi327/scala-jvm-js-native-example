/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.*
import cats.syntax.all.*

import ldbc.connector.data.Type

/**
 * Decoder of MySQL text-format data into Scala types.
 */
trait Decoder[A]:
  outer =>

  def types: List[Type]

  def length: Int = types.length

  def decode(offset: Int, ss: List[Option[String]]): Either[Decoder.Error, A]

  /** `Decoder` is semigroupal: a pair of decoders make a decoder for a pair. */
  def product[B](fb: Decoder[B]): Decoder[(A, B)] =
    new Decoder[(A, B)]:
      override val types: List[Type] = outer.types ++ fb.types
      override def decode(offset: Int, ss: List[Option[String]]): Either[Decoder.Error, (A, B)] =
        val (sa, sb) = ss.splitAt(outer.types.length)
        outer.decode(offset, sa) product fb.decode(offset + outer.length, sb)

object Decoder:

  /**
   * An error indicating that decoding a value starting at column `offset` and spanning `length`
   * columns failed with reason `error`.
   */
  case class Error(offset: Int, length: Int, message: String, `type`: Type, cause: Option[Throwable] = None)
