lazy val root = project
  .in(file("."))
  .settings(
    name := "just-another-ai-bot",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "io.cequence" %% "openai-scala-client-stream" % "1.0.0",
      "io.github.apimorphism" %% "telegramium-high" % "8.74.0",
      "org.tpolecat" %% "skunk-core" % "1.0.0-M6",
      "dev.rolang" %% "dumbo" % "0.3.3",
      "org.typelevel" %% "cats-effect" % "3.5.4",
      "org.typelevel" %% "cats-core" % "2.12.0",
      "co.fs2" %% "fs2-core" % "3.10.2",
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.7",
      "com.github.pureconfig" %% "pureconfig-generic-scala3" % "0.17.7",
      "org.log4s" %% "log4s" % "1.10.0",
      "org.slf4j" % "slf4j-simple" % "2.0.13",

      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "org.typelevel" %% "munit-cats-effect" % "2.0.0" % Test
    )
  )

enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

val scala3Version = "3.4.2"

dockerUpdateLatest := true
