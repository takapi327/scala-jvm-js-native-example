/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.util

object DataType:

  val MYSQL_TYPE_DECIMAL     = 0x00
  val MYSQL_TYPE_TINY        = 0x01
  val MYSQL_TYPE_SHORT       = 0x02
  val MYSQL_TYPE_LONG        = 0x03
  val MYSQL_TYPE_FLOAT       = 0x04
  val MYSQL_TYPE_DOUBLE      = 0x05
  val MYSQL_TYPE_NULL        = 0x06
  val MYSQL_TYPE_TIMESTAMP   = 0x07
  val MYSQL_TYPE_LONGLONG    = 0x08
  val MYSQL_TYPE_INT24       = 0x09
  val MYSQL_TYPE_DATE        = 0x0a
  val MYSQL_TYPE_TIME        = 0x0b
  val MYSQL_TYPE_DATETIME    = 0x0c
  val MYSQL_TYPE_YEAR        = 0x0d
  val MYSQL_TYPE_NEWDATE     = 0x0e
  val MYSQL_TYPE_VARCHAR     = 0x0f
  val MYSQL_TYPE_BIT         = 0x10
  val MYSQL_TYPE_TIMESTAMP2  = 0x11
  val MYSQL_TYPE_DATETIME2   = 0x12
  val MYSQL_TYPE_TIME2       = 0x13
  val MYSQL_TYPE_NEWDECIMAL  = 0xf6
  val MYSQL_TYPE_ENUM        = 0xf7
  val MYSQL_TYPE_SET         = 0xf8
  val MYSQL_TYPE_TINY_BLOB   = 0xf9
  val MYSQL_TYPE_MEDIUM_BLOB = 0xfa
  val MYSQL_TYPE_LONG_BLOB   = 0xfb
  val MYSQL_TYPE_BLOB        = 0xfc
  val MYSQL_TYPE_VAR_STRING  = 0xfd
  val MYSQL_TYPE_STRING      = 0xfe
  val MYSQL_TYPE_GEOMETRY    = 0xff
