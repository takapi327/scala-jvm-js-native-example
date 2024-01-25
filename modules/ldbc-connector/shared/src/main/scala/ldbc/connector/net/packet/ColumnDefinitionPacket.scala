/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.net.packet

import scodec.*
import scodec.codecs.*

import cats.syntax.all.*

case class ColumnDefinitionPacket(
  catalog: String,
  schema: Option[String],
  table: Option[String],
  orgTable: Option[String],
  name: Option[String],
  orgName: Option[String],
  length: Int,
  characterSet: Int,
  columnLength: Long,
  `type`: Int,
  flags: Int,
  decimals: Int,
) extends Packet:

  override def toString: String = "Protocol::ColumnDefinition41"

  def info: String = schema.getOrElse("") + table.fold("")("." + _) + name.fold("")("." + _) + " " + `type`

object ColumnDefinitionPacket:

  val decoder: Decoder[ColumnDefinitionPacket] =
    for
      catalogLength <- uint8
      catalog <- bytes(catalogLength).asDecoder
      schemaLength <- uint8
      schema <- bytes(schemaLength).asDecoder.map(_.decodeUtf8.getOrElse(""))
      tableLength <- uint8
      table <- bytes(tableLength).asDecoder.map(_.decodeUtf8.getOrElse(""))
      orgTableLength <- uint8
      orgTable <- bytes(orgTableLength).asDecoder.map(_.decodeUtf8.getOrElse(""))
      nameLength <- uint8
      name <- bytes(nameLength).asDecoder.map(_.decodeUtf8.getOrElse(""))
      orgNameLength <- uint8
      orgName <- bytes(orgNameLength).asDecoder.map(_.decodeUtf8.getOrElse(""))
      length <- uint8.asDecoder
      characterSet <- uint16.asDecoder
      columnLength <- uint32.asDecoder
      columnType <- uint8.asDecoder
      flags <- int(2).asDecoder
      decimals <- int(1).asDecoder
    yield ColumnDefinitionPacket(
      catalog = catalog.decodeUtf8.getOrElse(""),
      schema = if schema.isEmpty then None else schema.some,
      table = if table.isEmpty then None else table.some,
      orgTable = if orgTable.isEmpty then None else orgTable.some,
      name = if name.isEmpty then None else name.some,
      orgName = if orgName.isEmpty then None else orgName.some,
      length = length,
      characterSet = characterSet,
      columnLength = columnLength,
      `type` = columnType & 0xff,
      flags = flags,
      decimals = decimals
    )
