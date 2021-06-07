ThisBuild / version := "local"
ThisBuild / organization := "com.pirum"
ThisBuild / organizationName := "Pirum Systems"
ThisBuild / organizationHomepage := Some(url("https://www.pirum.com"))
ThisBuild / scalaVersion := "2.13.5"
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-Wunused:imports"
)

val akka = "2.6.14"
val log4j = "2.14.1"

lazy val workers = (project in file("."))
  .settings(
    name := "workers",
    Compile / mainClass := Option("com.pirum.exercises.worker.Main"),
    libraryDependencies ++= Seq(
      "org.apache.logging.log4j" % "log4j-core" % log4j,
      "org.apache.logging.log4j" % "log4j-api" % log4j,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.3",
      "com.typesafe.akka" %% "akka-actor-typed" % akka,
      "com.typesafe.akka" %% "akka-slf4j" % akka,
      
      "org.scalatest" %% "scalatest" % "3.2.9" % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akka % Test,
      "com.typesafe.akka" %% "akka-testkit" % akka % Test
    )
  )
