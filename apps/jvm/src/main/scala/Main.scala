/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import cats.effect.*

import fs2.*
import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*
import ldbc.connector.codec.all.*

object Main extends IOApp:

  given Tracer[IO] = Tracer.noop[IO]

  val session: Resource[IO, Session[IO]] =
    Session.single(
      host     = "127.0.0.1",
      port     = 13306,
      user     = "root",
      password = Some("root"),
      debug    = true,
      ssl      = SSL.None
    )

  override def run(args: List[String]): IO[ExitCode] =
    session.use { session =>
      for
        // result <- session.executeQuery("SELECT * FROM example.category")(
        //              bigint *: varchar *: varchar *: tinyint *: timestamp *: timestamp
        //            )
        preparedStatement <-
          session.clientPreparedStatement(
            "SELECT id, name, slug, color, p1, updated_at, created_at FROM example.category WHERE `id` = ? AND `name` = ? AND `date` <=> ?"
          )
        _ <- preparedStatement.setLong(1, 2L)
        _ <- preparedStatement.setString(2, "Category 2")
        // _ <- preparedStatement.setTime(java.time.LocalTime.of(22, 53, 55))
        // _ <- preparedStatement.setString(3, "category-2")
        // _ <- preparedStatement.setDate(2, java.time.LocalDate.of(2023, 10, 13))
        // _ <- preparedStatement.setTimestamp(java.time.LocalDateTime.of(2024, 2, 4, 22, 53, 55))
        _ <- preparedStatement.setNull(3)
        // _ <- preparedStatement.setShort(5, 1)
        result <-
          preparedStatement.executeQuery(bigint *: varchar *: varchar *: tinyint *: boolean *: timestamp *: timestamp)
            <* preparedStatement.close()
      yield
        result.foreach {
          case (id, name, slug, color, p1, updatedAt, createdAt) =>
            println(
              s"id: $id, name: $name, slug: $slug, color: $color, p1: $p1, updatedAt: $updatedAt, createdAt: $createdAt"
            )
        }
        ExitCode.Success
    }

import scala.util.Using
import com.mysql.cj.jdbc.*
object JDBC:

  val dataSource = new MysqlDataSource()
  dataSource.setServerName("127.0.0.1")
  dataSource.setPortNumber(13306)
  dataSource.setUser("root")
  dataSource.setPassword("root")
  dataSource.setUseSSL(false)

  @main def hoge(): Unit =
    Using
      .Manager { use =>
        val connection: JdbcConnection = use(dataSource.getConnection.asInstanceOf[JdbcConnection])
        // val statement = use(connection.clientPrepareStatement("SELECT * FROM example.category WHERE name = ?"))
        val statement = use(
          connection.clientPrepareStatement(
            "SELECT id, name, slug, color, p1, updated_at, created_at FROM example.category WHERE `id` = ? AND `name` = ? AND `date` >= ? AND slug = ? AND color = ?"
          )
        )
        // val statement = use(connection.createStatement())
        statement.setLong(1, 2L)
        statement.setString(2, "foo")
        statement.setDate(3, java.sql.Date.valueOf(java.time.LocalDate.of(2024, 10, 13)))
        statement.setNull(4, java.sql.Types.NULL)
        statement.setShort(5, 1)
        // statement.setTimestamp(5, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()))
        val resultSet = use(statement.executeQuery())
        // val resultSet = use(statement.executeQuery("SELECT * FROM example.category WHERE name = 'foo'"))
        val records = List.newBuilder[(Long, String, String, Short, java.sql.Timestamp, java.sql.Timestamp)]
        while resultSet.next() do {
          val code      = resultSet.getLong(1)
          val name      = resultSet.getString(2)
          val slug      = resultSet.getString(3)
          val color     = resultSet.getShort(4)
          val updatedAt = resultSet.getTimestamp(5)
          val createdAt = resultSet.getTimestamp(6)
          records += ((code, name, slug, color, updatedAt, createdAt))
        }
        println(records.result())
      }
      .getOrElse(throw new RuntimeException("Error during database operation"))
