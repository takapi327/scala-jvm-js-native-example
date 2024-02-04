/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

case class Parameter(
  columnDataType: ColumnDataType,
  value: None.type | Boolean | Byte | Short | Int | Long | Float | Double | BigDecimal | String | Array[Byte] | java.time.LocalTime | java.time.LocalDate | java.time.LocalDateTime
)
