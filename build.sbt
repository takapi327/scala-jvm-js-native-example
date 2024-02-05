import BuildSettings.*

ThisBuild / tlBaseVersion := "1.0"

ThisBuild / organization     := "io.github.takapi327"
ThisBuild / organizationName := "takapi327"
ThisBuild / startYear        := Some(2024)
ThisBuild / licenses         := Seq(License.MIT)
ThisBuild / developers := List(
  tlGitHubDev("takapi327", "takahiko tominaga")
)

ThisBuild / scalaVersion := "3.3.1"

ThisBuild / githubWorkflowPublish          := Seq.empty
ThisBuild / githubWorkflowPublishPreamble  := Seq.empty
ThisBuild / githubWorkflowPublishPostamble := Seq.empty
ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17")
)

lazy val compileSettings = Seq(
  tlFatalWarnings := true,

  // Headers
  headerMappings := headerMappings.value + (HeaderFileType.scala -> customCommentStyle),
  headerLicense := Some(
    HeaderLicense.Custom(
      """|Copyright (c) 2023-2024 by Takahiko Tominaga
       |This software is licensed under the MIT License (MIT).
       |For more information see LICENSE or https://opensource.org/licenses/MIT
       |""".stripMargin
    )
  )
)

lazy val connector = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .crossType(CrossType.Full)
  .in(file("modules/ldbc-connector"))
  .settings(compileSettings)
  .settings(
    name        := "ldbc-connector",
    description := "MySQL connector written in pure Scala3",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core"         % "2.10.0",
      "org.typelevel" %%% "cats-effect"       % "3.5.3",
      "co.fs2"        %%% "fs2-core"          % "3.10-365636d",
      "co.fs2"        %%% "fs2-io"            % "3.10-365636d",
      "org.scodec"    %%% "scodec-bits"       % "1.1.38",
      "org.scodec"    %%% "scodec-core"       % "2.2.2",
      "org.scodec"    %%% "scodec-cats"       % "1.2.0",
      "org.typelevel" %%% "otel4s-core-trace" % "0.4.0",
      "org.typelevel" %%% "twiddles-core"     % "0.8.0"
    )
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0"
    )
  )
  .enablePlugins(AutomateHeaderPlugin)

lazy val jvm = (project in file("apps/jvm"))
  .settings(name := "scala-jvm-example")
  .settings(compileSettings)
  .settings(run / fork := true)
  .settings(libraryDependencies += "com.mysql" % "mysql-connector-j" % "8.2.0")
  .dependsOn(connector.jvm)

lazy val js = (project in file("apps/js"))
  .settings(name := "scala-js-example")
  .settings(compileSettings)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    }
  )
  .dependsOn(connector.js)
  .enablePlugins(ScalaJSPlugin)

lazy val native = (project in file("apps/native"))
  .settings(name := "scala-native-example")
  .settings(compileSettings)
  // .settings(libraryDependencies ++= Seq(
  //  "com.armanbilge" %%% "epollcat" % "0.1.4"
  // ))
  .dependsOn(connector.native)
  .enablePlugins(ScalaNativePlugin)

lazy val root = (project in file("."))
  .settings(name := "scala-jvm-js-native-example")
  .settings(compileSettings)
  .aggregate(jvm, js, native, connector.jvm, connector.js, connector.native)
