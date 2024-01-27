/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

final case class Encoded(value: String, redacted: Boolean):

  override def toString: String = if redacted then Encoded.RedactedText else value

object Encoded:

  def apply(value: String): Encoded = Encoded(value, false)

  final val RedactedText: String = "?"
