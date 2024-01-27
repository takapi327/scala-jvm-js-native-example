/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.data

enum ColumnDataType(val code: Long):
  case MYSQL_TYPE_DECIMAL extends ColumnDataType(0x00)
  case MYSQL_TYPE_TINY extends ColumnDataType(0x01)
  case MYSQL_TYPE_SHORT extends ColumnDataType(0x02)
  case MYSQL_TYPE_LONG extends ColumnDataType(0x03)
  case MYSQL_TYPE_FLOAT extends ColumnDataType(0x04)
  case MYSQL_TYPE_DOUBLE extends ColumnDataType(0x05)
  case MYSQL_TYPE_NULL extends ColumnDataType(0x06)
  case MYSQL_TYPE_TIMESTAMP extends ColumnDataType(0x07)
  case MYSQL_TYPE_LONGLONG extends ColumnDataType(0x08)
  case MYSQL_TYPE_INT24 extends ColumnDataType(0x09)
  case MYSQL_TYPE_DATE extends ColumnDataType(0x0a)
  case MYSQL_TYPE_TIME extends ColumnDataType(0x0b)
  case MYSQL_TYPE_DATETIME extends ColumnDataType(0x0c)
  case MYSQL_TYPE_YEAR extends ColumnDataType(0x0d)
  case MYSQL_TYPE_NEWDATE extends ColumnDataType(0x0e)
  case MYSQL_TYPE_VARCHAR extends ColumnDataType(0x0f)
  case MYSQL_TYPE_BIT extends ColumnDataType(0x10)
  case MYSQL_TYPE_TIMESTAMP2 extends ColumnDataType(0x11)
  case MYSQL_TYPE_DATETIME2 extends ColumnDataType(0x12)
  case MYSQL_TYPE_TIME2 extends ColumnDataType(0x13)
  case MYSQL_TYPE_NEWDECIMAL extends ColumnDataType(0xf6)
  case MYSQL_TYPE_ENUM extends ColumnDataType(0xf7)
  case MYSQL_TYPE_SET extends ColumnDataType(0xf8)
  case MYSQL_TYPE_TINY_BLOB extends ColumnDataType(0xf9)
  case MYSQL_TYPE_MEDIUM_BLOB extends ColumnDataType(0xfa)
  case MYSQL_TYPE_LONG_BLOB extends ColumnDataType(0xfb)
  case MYSQL_TYPE_BLOB extends ColumnDataType(0xfc)
  case MYSQL_TYPE_VAR_STRING extends ColumnDataType(0xfd)
  case MYSQL_TYPE_STRING extends ColumnDataType(0xfe)
  case MYSQL_TYPE_GEOMETRY extends ColumnDataType(0xff)

object ColumnDataType:
  
  def apply(code: Long): ColumnDataType =
    ColumnDataType.values.find(_.code == code).getOrElse(throw new IllegalArgumentException(s"Unknown column data type code: $code"))
