/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import cats.syntax.all.*

import ldbc.connector.Codec
import ldbc.connector.data.Type

trait TextCodecs:

  val varchar: Codec[String] = Codec.simple(s => s, _.asRight, Type.varchar)
  def varchar(size: Int): Codec[String] = Codec.simple(s => s, _.asRight, Type.varchar(size))

object text extends TextCodecs
