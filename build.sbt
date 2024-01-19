import Dependencies.*

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

lazy val compileSettings = Def.settings(
  // 警告をエラーにする
  tlFatalWarnings := true,

  // デフォルトで設定されるがうまくいかないものを外す
  scalacOptions --= Seq(
    // Scala 3.0.1以降だとうまく動かない
    // https://github.com/lampepfl/dotty/issues/14952
    "-Ykind-projector:underscores",
  ),
  Test / scalacOptions --= Seq(
    // テストだとちょっと厳しすぎる
    "-Wunused:locals",
  ),
  Compile / console / scalacOptions --= Seq(
    // コンソールで import した瞬間はまだ使ってないから当然許したい
    "-Wunused:imports",
  ),
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

lazy val root = tlCrossRootProject
  .settings(name := "scala-jvm-js-native-example")
  .settings(compileSettings)
  .aggregate(connector)
