import sbt.*

object Dependencies {

  val cats       = "org.typelevel" %% "cats-core"   % "2.10.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.2"

  val fs2 = Seq(
    "fs2-core",
    "fs2-io"
  ).map("co.fs2" %% _ % "3.9.4")

  val ip4s = "com.comcast" %% "ip4s-core" % "3.2.0"
}
