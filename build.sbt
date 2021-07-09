name := "common-log-analyzer"
version := "1.0.0"
scalaVersion := "2.13.6"

assembly / mainClass := Some("rminhas.MainApp")
assembly / test := (Test / test).value  // run the tests before generating the jar

val AkkaVersion = "2.6.14"
libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-file" % "3.0.1",
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.1.4" % Test
)

