/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.codecs.*

import cats.syntax.all.*

/**
 *
 * @param catalog
 *   Type: string<lenenc>
 *   Name: catalog
 *   Description: The catalog used. Currently always "def"
 * @param schema
 *   Type: string<lenenc>
 *   Name: schema
 *   Description: schema name
 * @param table
 *   Type: string<lenenc>
 *   Name: table
 *   Description: virtual table name
 * @param orgTable
 *   Type: string<lenenc>
 *   Name: org_table
 *   Description: physical table name
 * @param name
 *   Type: string<lenenc>
 *   Name: name
 *   Description: virtual column name
 * @param orgName
 *   Type: string<lenenc>
 *   Name: org_name
 *   Description: physical column name
 * @param length
 *   Type: int<lenenc>
 *   Name: length of fixed length fields
 *   Description: 0x0c
 * @param characterSet
 *   Type: int<2>
 *   Name: character_set
 *   Description: the column character set as defined in Character Set
 * @param columnLength
 *   Type: int<4>
 *   Name: column_length
 *   Description: maximum length of the field
 * @param columnType
 *   Type: int<1>
 *   Name: type
 *   Description: type of the column as defined in enum_field_types
 * @param flags
 *   Type: int<2>
 *   Name: flags
 *   Description: Flags as defined in Column Definition Flags
 * @param decimals
 *   Type: int<1>
 *   Name: decimals
 *   Description: max shown decimal digits
 *     - 0x00 for integers and static strings
 *     - 0x1f for dynamic strings, double, float
 *     - 0x00 to 0x51 for decimals
 */
case class ColumnDefinitionPacket(
  catalog:      String,
  schema:       Option[String],
  table:        Option[String],
  orgTable:     Option[String],
  name:         Option[String],
  orgName:      Option[String],
  length:       Int,
  characterSet: Int,
  columnLength: Long,
  columnType:   Int,
  flags:        Int,
  decimals:     Int
) extends Packet:

  override def toString: String = "Protocol::ColumnDefinition41"

  def info: String =
    schema.getOrElse("") + table.fold("")("." + _) + name.fold("")("." + _) + "Data Type Code: " + columnType

object ColumnDefinitionPacket:

  val decoder: Decoder[ColumnDefinitionPacket] =
    for
      catalogLength  <- uint8
      catalog        <- bytes(catalogLength).asDecoder
      schemaLength   <- uint8
      schema         <- bytes(schemaLength).asDecoder.map(_.decodeUtf8.getOrElse(""))
      tableLength    <- uint8
      table          <- bytes(tableLength).asDecoder.map(_.decodeUtf8.getOrElse(""))
      orgTableLength <- uint8
      orgTable       <- bytes(orgTableLength).asDecoder.map(_.decodeUtf8.getOrElse(""))
      nameLength     <- uint8
      name           <- bytes(nameLength).asDecoder.map(_.decodeUtf8.getOrElse(""))
      orgNameLength  <- uint8
      orgName        <- bytes(orgNameLength).asDecoder.map(_.decodeUtf8.getOrElse(""))
      length         <- uint8.asDecoder
      characterSet   <- uint16.asDecoder
      columnLength   <- uint32.asDecoder
      columnType     <- uint8.asDecoder
      flags          <- int(2).asDecoder
      decimals       <- int(1).asDecoder
    yield ColumnDefinitionPacket(
      catalog      = catalog.decodeUtf8.getOrElse(""),
      schema       = if schema.isEmpty then None else schema.some,
      table        = if table.isEmpty then None else table.some,
      orgTable     = if orgTable.isEmpty then None else orgTable.some,
      name         = if name.isEmpty then None else name.some,
      orgName      = if orgName.isEmpty then None else orgName.some,
      length       = length,
      characterSet = characterSet,
      columnLength = columnLength,
      columnType   = columnType,
      flags        = flags,
      decimals     = decimals
    )
