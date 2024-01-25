package ldbc.connector.util

object DataType:
  
  private val MYSQL_TYPE_DECIMAL = 0x00
  private val MYSQL_TYPE_TINY = 0x01
  private val MYSQL_TYPE_SHORT = 0x02
  private val MYSQL_TYPE_LONG = 0x03
  private val MYSQL_TYPE_FLOAT = 0x04
  private val MYSQL_TYPE_DOUBLE = 0x05
  private val MYSQL_TYPE_NULL = 0x06
  private val MYSQL_TYPE_TIMESTAMP = 0x07
  private val MYSQL_TYPE_LONGLONG = 0x08
  private val MYSQL_TYPE_INT24 = 0x09
  private val MYSQL_TYPE_DATE = 0x0a
  private val MYSQL_TYPE_TIME = 0x0b
  private val MYSQL_TYPE_DATETIME = 0x0c
  private val MYSQL_TYPE_YEAR = 0x0d
  private val MYSQL_TYPE_NEWDATE = 0x0e
  private val MYSQL_TYPE_VARCHAR = 0x0f
  private val MYSQL_TYPE_BIT = 0x10
  private val MYSQL_TYPE_TIMESTAMP2 = 0x11
  private val MYSQL_TYPE_DATETIME2 = 0x12
  private val MYSQL_TYPE_TIME2 = 0x13
  private val MYSQL_TYPE_NEWDECIMAL = 0xf6
  private val MYSQL_TYPE_ENUM = 0xf7
  private val MYSQL_TYPE_SET = 0xf8
  private val MYSQL_TYPE_TINY_BLOB = 0xf9
  private val MYSQL_TYPE_MEDIUM_BLOB = 0xfa
  private val MYSQL_TYPE_LONG_BLOB = 0xfb
  private val MYSQL_TYPE_BLOB = 0xfc
  private val MYSQL_TYPE_VAR_STRING = 0xfd
  private val MYSQL_TYPE_STRING = 0xfe
  private val MYSQL_TYPE_GEOMETRY = 0xff
