/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

//import cats.Monad
import cats.data.StateT
import cats.syntax.all.*

import cats.effect.*

import ldbc.connector.net.message.*
import ldbc.connector.net.packet.*
import ldbc.connector.util.DataType

class PreparedStatement[F[_]: Concurrent](statementId: Long, numParams: Int, bms: BufferedMessageSocket[F]):

  def setInt(index: Int, value: Int): PreparedStatement.QueryState[F, Unit] =
    StateT.modify[F, Map[Int, String]](_ + (index -> value.toString))

  def setString(index: Int, value: String): PreparedStatement.QueryState[F, Unit] =
    StateT.modify[F, Map[Int, String]](_ + (index -> s"'$value'"))

  private def repeatProcess[P <: Packet](times: Int, decoder: scodec.Decoder[P]): F[List[P]] =
    def read(remaining: Int, acc: List[P]): F[List[P]] =
      if remaining <= 0 then Concurrent[F].pure(acc)
      else bms.receive(decoder).flatMap(result => read(remaining - 1, acc :+ result))

    read(times, List.empty[P])

  private def readUntilEOF(
    columns: List[ColumnDefinitionPacket],
    acc: List[BinaryProtocolResultSetRowPacket]
  ): F[List[BinaryProtocolResultSetRowPacket]] =
    bms.receive(BinaryProtocolResultSetRowPacket.decoder(columns)).flatMap {
      case _: EOFPacket => Concurrent[F].pure(acc)
      case _: ERRPacket => Concurrent[F].raiseError(new RuntimeException("Error packet received"))
      case row: BinaryProtocolResultSetRowPacket => readUntilEOF(columns, acc :+ row)
    }

  def executeQuery[A](codec: ldbc.connector.Codec[A]): F[List[A]] =
    for
      columnCount <- bms.changeCommandPhase *> bms.send(
        ComStmtExecute(
          statementId,
          numParams,
          Map(DataType.MYSQL_TYPE_LONGLONG -> 1L, DataType.MYSQL_TYPE_VAR_STRING -> "Category 1")
        )
      ) *> bms.receive(ColumnsNumberPacket.decoder)
      columns      <- repeatProcess(columnCount.columnCount, ColumnDefinitionPacket.decoder)
      resultSetRow <- readUntilEOF(columns, Nil)
    yield resultSetRow.map(row =>
      codec.decode(0, row.value) match
        case Left(value) =>
          val column = columns(value.offset)
          throw new IllegalArgumentException(s"""
                                                |==========================
                                                |Failed to decode column: `${ column.name }`
                                                |Decode To: ${ column.columnType } -> ${ value.`type`.name.toUpperCase }
                                                |
                                                |Message [ ${ value.message } ]
                                                |==========================
                                                |""".stripMargin)
        case Right(value) => value
    )

object PreparedStatement:

  type QueryState[F[_], A] = StateT[F, Map[Int, String], A]
