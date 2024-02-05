/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector

import scala.collection.immutable.ListMap

import cats.syntax.all.*

import cats.effect.*

import ldbc.connector.net.message.*
import ldbc.connector.net.packet.*
import ldbc.connector.data.*

trait PreparedStatement[F[_]: Concurrent]:

  def bms: BufferedMessageSocket[F]
  def params: Ref[F, ListMap[Int, Parameter]]

  def setNull(index: Int): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_NULL, None)))

  def setBoolean(index: Int, value: Boolean): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_TINY, value)))

  def setByte(index: Int, value: Byte): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_TINY, value)))

  def setShort(index: Int, value: Short): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_SHORT, value)))

  def setInt(index: Int, value: Int): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_LONG, value)))

  def setLong(index: Int, value: Long): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_LONGLONG, value)))

  def setFloat(index: Int, value: Float): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_FLOAT, value)))

  def setDouble(index: Int, value: Double): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_DOUBLE, value)))

  def setBigDecimal(index: Int, value: BigDecimal): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_NEWDECIMAL, value)))

  def setString(index: Int, value: String): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_VAR_STRING, value)))

  def setBytes(index: Int, value: Array[Byte]): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_VAR_STRING, value)))

  def setTime(index: Int, value: java.time.LocalTime): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_TIME, value)))

  def setDate(index: Int, value: java.time.LocalDate): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_DATE, value)))

  def setTimestamp(index: Int, value: java.time.LocalDateTime): F[Unit] =
    params.update(_ + (index -> Parameter(ColumnDataType.MYSQL_TYPE_TIMESTAMP, value)))

  protected def repeatProcess[P <: Packet](times: Int, decoder: scodec.Decoder[P]): F[List[P]] =
    def read(remaining: Int, acc: List[P]): F[List[P]] =
      if remaining <= 0 then Concurrent[F].pure(acc)
      else bms.receive(decoder).flatMap(result => read(remaining - 1, acc :+ result))

    read(times, List.empty[P])

  protected def readUntilEOF[P <: Packet](
    columns: List[ColumnDefinitionPacket],
    decoder: List[ColumnDefinitionPacket] => scodec.Decoder[P | EOFPacket | ERRPacket],
    acc:     List[P]
  ): F[List[P]] =
    bms.receive(decoder(columns)).flatMap {
      case _: EOFPacket => Concurrent[F].pure(acc)
      case _: ERRPacket => Concurrent[F].raiseError(new RuntimeException("Error packet received"))
      case row          => readUntilEOF(columns, decoder, acc :+ row.asInstanceOf[P])
    }

  def executeQuery[A](codec: ldbc.connector.Codec[A]): F[List[A]]

  def close(): F[Unit]

object PreparedStatement:

  case class Client[F[_]: Concurrent](
    bms: BufferedMessageSocket[F],
    sql: String,
    params: Ref[
      F,
      ListMap[Int, Parameter]
    ],
    capabilityFlags: Seq[CapabilitiesFlags]
  ) extends PreparedStatement[F]:

    private def buildQuery(
      params: ListMap[Int, Parameter]
    ): String =
      val query = sql.toCharArray
      params
        .foldLeft(query) {
          case (query, (offset, param)) =>
            val index = query.indexOf('?', offset - 1)
            if index < 0 then query
            else
              val (head, tail)         = query.splitAt(index)
              val (tailHead, tailTail) = tail.splitAt(1)
              val newValue = param.value match
                case None          => "NULL".toCharArray
                case v: Boolean    => v.toString.toCharArray
                case v: Byte       => v.toString.toCharArray
                case v: Short      => v.toString.toCharArray
                case v: Int        => v.toString.toCharArray
                case v: Long       => v.toString.toCharArray
                case v: Float      => v.toString.toCharArray
                case v: Double     => v.toString.toCharArray
                case v: BigDecimal => v.toString.toCharArray
                case v: String     => "'".toCharArray ++ v.toCharArray ++ "'".toCharArray
                case v: Array[Byte] =>
                  val hex = v.map("%02x".format(_)).mkString
                  "X'".toCharArray ++ hex.toCharArray ++ "'".toCharArray
                case v: java.time.LocalTime     => "'".toCharArray ++ v.toString.toCharArray ++ "'".toCharArray
                case v: java.time.LocalDate     => "'".toCharArray ++ v.toString.toCharArray ++ "'".toCharArray
                case v: java.time.LocalDateTime => "'".toCharArray ++ v.toString.toCharArray ++ "'".toCharArray
              head ++ newValue ++ tailTail
        }
        .mkString

    override def executeQuery[A](codec: ldbc.connector.Codec[A]): F[List[A]] =
      for
        params <- params.get
        columnCount <- bms.changeCommandPhase *>
                         bms.send(ComQuery(buildQuery(params), capabilityFlags, Map.empty)) *>
                         bms.receive(ColumnsNumberPacket.decoder).flatMap {
                           case error: ERRPacket =>
                             Concurrent[F]
                               .raiseError(new Exception(s"Failed to execute query: ${ error.errorMessage }"))
                           case result: ColumnsNumberPacket => Concurrent[F].pure(result)
                         }
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

    override def close(): F[Unit] = Concurrent[F].unit

  case class Server[F[_]: Concurrent](
    statementId: Long,
    bms:         BufferedMessageSocket[F],
    params: Ref[
      F,
      ListMap[Int, Parameter]
    ]
  ) extends PreparedStatement[F]:

    override def executeQuery[A](codec: ldbc.connector.Codec[A]): F[List[A]] =
      for
        params <- params.get
        columnCount <- bms.changeCommandPhase *> bms.send(
                         ComStmtExecute(
                           statementId,
                           params
                         )
                       ) *> bms.receive(ColumnsNumberPacket.decoder).flatMap {
                         case error: ERRPacket =>
                           Concurrent[F].raiseError(new Exception(s"Failed to execute query: ${ error.errorMessage }"))
                         case result: ColumnsNumberPacket => Concurrent[F].pure(result)
                       }
        columns <- repeatProcess(columnCount.columnCount, ColumnDefinitionPacket.decoder)
        resultSetRow <-
          readUntilEOF[BinaryProtocolResultSetRowPacket](columns, BinaryProtocolResultSetRowPacket.decoder, Nil)
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

    override def close(): F[Unit] = bms.changeCommandPhase *> bms.send(ComStmtClose(statementId))
