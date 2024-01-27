/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

import cats.Eq
import cats.syntax.all.*

final case class Type(name: String, componentTypes: List[Type] = Nil)

object Type:

  given EqType: Eq[Type] = Eq.fromUniversalEquals

  // @deprecated("Use Type.int instead", "0.1.0")
  def tinyint(n: Int): Type = Type(s"tinyint($n)")
  val tinyint: Type = Type("tinyint")

  // @deprecated("Use Type.bigint instead", "0.1.0")
  def bigint(n: Int): Type = Type(s"bigint($n)")
  val bigint: Type = Type("bigint")

  def varchar(n: Int): Type = Type(s"varchar($n)")
  val varchar: Type = Type("varchar")

  def time(n: Int): Type = Type(s"time($n)")
  val time: Type = Type("time")

  def timestamp(n: Int): Type = Type(s"timestamp($n)")
  val timestamp: Type = Type("timestamp")
