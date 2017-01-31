name := """spacelift"""

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "space.spacelift" %% "amqp-scala-client" % "2.0.0",
  "space.spacelift" %% "akka-mq-proxies" % "2.0.0-SNAPSHOT",
  "space.spacelift" %% "akka-mq-proxies-amqp" % "2.0.0-SNAPSHOT",
  "com.typesafe.akka" %% "akka-actor" % "2.4.16",
  "com.typesafe.akka" %% "akka-http" % "10.0.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.16",
  "junit" % "junit" % "4.12" % "test",
  "javax.inject" % "javax.inject" % "1"
)

