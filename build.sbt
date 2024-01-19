import Dependencies._
import BuildSettings._

ThisBuild / tlBaseVersion := "1.0"

ThisBuild / organization     := "io.github.takapi327"
ThisBuild / organizationName := "takapi327"
ThisBuild / startYear        := Some(2024)
ThisBuild / licenses         := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("takapi327", "takahiko tominaga"),
)

ThisBuild / scalaVersion := "3.3.1"

ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17"),
)

lazy val compileSettings = Seq(
  tlFatalWarnings := true,

  // Headers
  headerMappings := headerMappings.value + (HeaderFileType.scala -> customCommentStyle),
  headerLicense  := Some(HeaderLicense.Custom(
    """|Copyright (c) 2023-2024 by Takahiko Tominaga
       |This software is licensed under the MIT License (MIT).
       |For more information see LICENSE or https://opensource.org/licenses/MIT
       |""".stripMargin
  )),
)

lazy val connector = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("modules/ldbc-connector"))
  .settings(compileSettings)
  .settings(
    name := "ldbc-connector",
    description := "MySQL connector for native Scala",
    libraryDependencies ++= Seq(
      cats,
      catsEffect,
      "org.scodec" %%% "scodec-bits" % "1.1.38",
      "org.scodec" %%% "scodec-core" % "2.2.2",
      "org.scodec" %%% "scodec-cats" % "1.2.0",
    )
  )
  .enablePlugins(AutomateHeaderPlugin)

lazy val root = tlCrossRootProject
  .settings(name := "scala-jvm-js-native-example")
  .settings(compileSettings)
  .aggregate(connector)
