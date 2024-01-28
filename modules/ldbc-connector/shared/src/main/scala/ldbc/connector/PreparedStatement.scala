/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.Monad
import cats.data.StateT
import cats.syntax.all.*

import ldbc.connector.net.message.ComStmtExecute

class PreparedStatement[F[_]: Monad](statementId: Long, numParams: Int, bms: BufferedMessageSocket[F]):

  def setInt(index: Int, value: Int): PreparedStatement.QueryState[F, Unit] =
    StateT.modify[F, Map[Int, String]](_ + (index -> value.toString))

  def setString(index: Int, value: String): PreparedStatement.QueryState[F, Unit] =
    StateT.modify[F, Map[Int, String]](_ + (index -> s"'$value'"))

  def executeQuery(): F[Unit] =
    bms.changeCommandPhase *> bms.send(ComStmtExecute(statementId, numParams, List("foo")))

object PreparedStatement:

  type QueryState[F[_], A] = StateT[F, Map[Int, String], A]
