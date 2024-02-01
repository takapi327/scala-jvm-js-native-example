/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import ldbc.connector.Codec
import ldbc.connector.data.Type

trait NumericCodecs:

  private def safe[A](f: String => A): String => Either[String, A] = s =>
    try Right(f(s))
    catch {
      case _: NumberFormatException => Left(s"Invalid: $s")
    }

  val tinyint: Codec[Short] = Codec.simple(_.toString, safe(_.toShort), Type.tinyint)
  def tinyint(n: Int): Codec[Short] = Codec.simple(_.toString, safe(_.toShort), Type.tinyint(n))

  val bigint: Codec[Long] = Codec.simple(_.toString, safe(_.toLong), Type.bigint)
  def bigint(n: Int): Codec[Long] = Codec.simple(_.toString, safe(_.toLong), Type.bigint(n))

object numeric extends NumericCodecs
