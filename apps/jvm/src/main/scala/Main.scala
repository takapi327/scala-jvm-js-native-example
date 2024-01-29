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
        preparedStatement <- session.preparedStatement("SELECT * FROM example.category WHERE id = ? & name = ?")
        result            <- preparedStatement.executeQuery(bigint *: varchar *: varchar *: tinyint *: timestamp *: timestamp)
      yield
        result.foreach {
         case (id, name, slug, color, updatedAt, createdAt) =>
           println(s"id: $id, name: $name, slug: $slug, color: $color, updatedAt: $updatedAt, createdAt: $createdAt")
        }
        ExitCode.Success
    }

import scala.util.Using
import com.mysql.cj.jdbc.*
//import software.aws.rds.jdbc.mysql.shading.com.mysql.cj.jdbc.MysqlDataSource
//import software.aws.rds.jdbc.mysql.shading.com.mysql.cj.jdbc.ServerPreparedStatement
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
        // val statement = use(connection.prepareStatement("SELECT * FROM example.category WHERE name = ?"))
        // val statement = use(ServerPreparedStatement.getInstance(connection, "SELECT * FROM example.category WHERE name = ?", "example", 0, 0))
        val statement = connection.serverPrepareStatement("SELECT * FROM example.category WHERE id = ? & name = ?")
        // val statement = use(connection.createStatement())
        statement.setLong(1, 1L)
        statement.setString(2, "foo")
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
