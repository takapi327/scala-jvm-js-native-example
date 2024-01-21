/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.exception

import org.typelevel.otel4s.Attribute

class LdbcException(
  sql:      Option[String],
  message:  String,
  position: Option[Int]                   = None,
  detail:   Option[String]                = None,
  hint:     Option[String]                = None,
  //history:  List[Either[Any, Any]]        = Nil,
  //arguments:       List[(Type, Option[Encoded])] = Nil,
  //sqlOrigin:       Option[Origin]                = None,
  //argumentsOrigin: Option[Origin]                = None,
  //callSite:        Option[CallSite]              = None
) extends Exception(message):

  def fields: List[Attribute[?]] =
    val builder = List.newBuilder[Attribute[?]]

    builder += Attribute("error.message", message)

    sql.foreach(a => builder += Attribute("error.sql", a))
    position.foreach(a => builder += Attribute("error.position", a.toLong))
    detail.foreach(a => builder += Attribute("error.detail", a))
    hint.foreach(a => builder += Attribute("error.hint", a))

    builder.result()
