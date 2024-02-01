/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import cats.syntax.all.*

import cats.effect.*

import ldbc.connector.net.message.*
import ldbc.connector.net.packet.*
import ldbc.connector.util.DataType
import ldbc.connector.data.CapabilitiesFlags

trait PreparedStatement[F[_]: Concurrent]:

  def bms:         BufferedMessageSocket[F]
  def params:      Ref[F, Map[Int, Long | String]]

  def setLong(value: Long): F[Unit] =
    params.update(_ + (DataType.MYSQL_TYPE_LONGLONG -> value))

  def setString(value: String): F[Unit] =
    params.update(_ + (DataType.MYSQL_TYPE_VAR_STRING -> value))

  protected def repeatProcess[P <: Packet](times: Int, decoder: scodec.Decoder[P]): F[List[P]] =
    def read(remaining: Int, acc: List[P]): F[List[P]] =
      if remaining <= 0 then Concurrent[F].pure(acc)
      else bms.receive(decoder).flatMap(result => read(remaining - 1, acc :+ result))

    read(times, List.empty[P])

  protected def readUntilEOF[P <: Packet](
    columns: List[ColumnDefinitionPacket],
    decoder: List[ColumnDefinitionPacket] => scodec.Decoder[P | EOFPacket | ERRPacket],
    acc: List[P]
  ): F[List[P]] =
    bms.receive(decoder(columns)).flatMap {
      case _: EOFPacket => Concurrent[F].pure(acc)
      case _: ERRPacket => Concurrent[F].raiseError(new RuntimeException("Error packet received"))
      case row => readUntilEOF(columns, decoder, acc :+ row.asInstanceOf[P])
    }

  def executeQuery[A](codec: ldbc.connector.Codec[A]): F[List[A]]

object PreparedStatement:

  case class Client[F[_]: Concurrent](
    bms:         BufferedMessageSocket[F],
    sql:         String,
    params:      Ref[F, Map[Int, Long | String]],
    capabilityFlags: Seq[CapabilitiesFlags]
  ) extends PreparedStatement[F]:

    private def buildQuery(params: Map[Long | String, Int]): String =
      val query = sql.toCharArray
      params.foldLeft(query) { case (query, (value, offset)) =>
        val index = query.indexOf('?', offset)
        if index < 0 then query
        else
          val (head, tail) = query.splitAt(index)
          val (tailHead, tailTail) = tail.splitAt(1)
          val newValue = value match
            case v: Long   => v.toString.toCharArray
            case v: String => "'".toCharArray ++ v.toCharArray ++ "'".toCharArray
          head ++ newValue ++ tailTail
      }.mkString

    override def executeQuery[A](codec: ldbc.connector.Codec[A]): F[List[A]] =
      for
        params <- params.get
        columnCount <- bms.changeCommandPhase *>
          bms.send(ComQuery(buildQuery(params.values.zipWithIndex.toMap), capabilityFlags, Map.empty)) *>
          bms.receive(ColumnsNumberPacket.decoder)
        columns      <- repeatProcess(columnCount.columnCount, ColumnDefinitionPacket.decoder)
        resultSetRow <- readUntilEOF[ResultSetRowPacket](columns, ResultSetRowPacket.decoder, Nil)
      yield resultSetRow
        .map(row =>
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

  case class Server[F[_]: Concurrent](
    statementId: Long,
    numParams:   Int,
    bms:         BufferedMessageSocket[F],
    params:      Ref[F, Map[Int, Long | String]]
  ) extends PreparedStatement[F]:

    override def executeQuery[A](codec: ldbc.connector.Codec[A]): F[List[A]] =
      for
        params <- params.get
        columnCount <- bms.changeCommandPhase *> bms.send(
          ComStmtExecute(
            statementId,
            numParams,
            params
          )
        ) *> bms.receive(ColumnsNumberPacket.decoder)
        columns      <- repeatProcess(columnCount.columnCount, ColumnDefinitionPacket.decoder)
        resultSetRow <- readUntilEOF[BinaryProtocolResultSetRowPacket](columns, BinaryProtocolResultSetRowPacket.decoder, Nil)
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
