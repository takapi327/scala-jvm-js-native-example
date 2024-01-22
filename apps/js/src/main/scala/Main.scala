/**
 * Copyright (c) 2023-2024 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

import scala.concurrent.duration.*

import cats.effect.*

import fs2.*
import fs2.io.net.*

import org.typelevel.otel4s.trace.Tracer

import ldbc.connector.*

object Main extends IOApp:

  given Tracer[IO] = Tracer.noop[IO]

  val session: Resource[IO, Session[IO]] =
    Session.single(
      host = "127.0.0.1",
      port = 13306,
      user = "root",
      password = Some("root"),
      debug = true,
      ssl = SSL.None,
    )

  override def run(args: List[String]): IO[ExitCode] =
    session.use { session =>
      IO.sleep(5.seconds) *>
        IO.pure(ExitCode.Success)
    }
