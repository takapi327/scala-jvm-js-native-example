ThisBuild / organization := "io.github.takapi327"
ThisBuild / startYear := Some(2024)

lazy val root = (project in file("."))
  .settings(
    scalaVersion := "3.3.1",
    run / fork := true,
  )

