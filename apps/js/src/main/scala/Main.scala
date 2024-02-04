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
        preparedStatement <- session.clientPreparedStatement(
                               "SELECT * FROM example.category WHERE p1 = ?"
                             )
        _ <- preparedStatement.setBoolean(1, false) // *> preparedStatement.setString("Category 1")
        result <-
          preparedStatement.executeQuery(bigint *: varchar *: varchar *: tinyint *: boolean *: timestamp *: timestamp)
      yield
        result.foreach {
          case (id, name, slug, color, p1, updatedAt, createdAt) =>
            println(
              s"id: $id, name: $name, slug: $slug, color: $color, p1: $p1, updatedAt: $updatedAt, createdAt: $createdAt"
            )
        }
        ExitCode.Success
    }
