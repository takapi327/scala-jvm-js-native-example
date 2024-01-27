/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.codec

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
//import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
//import java.time.OffsetDateTime
//import java.time.OffsetTime
import java.time.temporal.ChronoField._
import java.time.temporal.TemporalAccessor
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
//import java.time.Duration
import java.util.Locale

import cats.syntax.all.*

import ldbc.connector.Codec
import ldbc.connector.data.Type

trait TemporalCodecs:

  private def temporal[A <: TemporalAccessor](
    formatter: DateTimeFormatter,
    parse: (String, DateTimeFormatter) => A,
    tpe: Type
  ): Codec[A] =
    Codec.simple(
      a => formatter.format(a),
      s => Either.catchOnly[DateTimeParseException](parse(s, formatter)).leftMap(_.toString),
      tpe
    )

  private def timeFormatter(precision: Int): DateTimeFormatter =

    val requiredPart: DateTimeFormatterBuilder =
      new DateTimeFormatterBuilder()
        .appendValue(HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 2)

    if precision > 0 then
      requiredPart
        .optionalStart
        .appendFraction(NANO_OF_SECOND, 0, precision, true)
        .optionalEnd
      ()

    requiredPart.toFormatter(Locale.US)

  private val localDateFormatterWithoutEra: DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .appendValue(YEAR_OF_ERA, 4, 19, SignStyle.NOT_NEGATIVE)
      .appendLiteral('-')
      .appendValue(MONTH_OF_YEAR, 2)
      .appendLiteral('-')
      .appendValue(DAY_OF_MONTH, 2)
      .toFormatter(Locale.US)

  private val eraFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern(" G", Locale.US)

  private def localDateTimeFormatter(precision: Int): DateTimeFormatter =
    new DateTimeFormatterBuilder()
      .append(localDateFormatterWithoutEra)
      .appendLiteral(' ')
      .append(timeFormatter(precision))
      .appendOptional(eraFormatter)
      .toFormatter(Locale.US)

  val time: Codec[LocalTime] =
    temporal(timeFormatter(6), LocalTime.parse, Type.time)

  def time(precision: Int): Codec[LocalTime] =
    if precision >= 0 && precision <= 6 then
      temporal(timeFormatter(precision), LocalTime.parse, Type.time(precision))
    else
      throw new IllegalArgumentException(s"time($precision): invalid precision, expected 0-6")

  val timestamp: Codec[LocalDateTime] =
    temporal(localDateTimeFormatter(6), LocalDateTime.parse, Type.timestamp)
