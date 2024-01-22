
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

/*
import scala.scalajs.js.annotation.*

object Main:

  @JSExportTopLevel(name = "handler", moduleID = "index")
  def hello(): Unit =
    println("Hello world!")
    println(msg)

  def msg = "I was compiled by Scala 3. :)"
 */
