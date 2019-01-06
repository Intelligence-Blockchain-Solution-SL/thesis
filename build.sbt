organization := "es.ibs"

name := "thesis"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.7"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-explaintypes", "-encoding", "utf-8",
  "-Xlint", "-Xfatal-warnings")

lazy val akkaVer = "2.5.19"
lazy val guiceVer = "4.2.2"
libraryDependencies ++= Seq(
  // java
  "ch.qos.logback"                %  "logback-classic"          % "1.2.3",
  "com.google.inject.extensions"  %  "guice-assistedinject"     % guiceVer,
  "org.postgresql"                %  "postgresql"               % "42.2.5",
  // scala
  "com.typesafe.scala-logging"    %% "scala-logging"            % "3.9.2",
  "net.codingwell"                %% "scala-guice"              % guiceVer,
  "com.typesafe.akka"             %% "akka-actor"               % akkaVer,
  "com.typesafe.akka"             %% "akka-slf4j"               % akkaVer,
  "com.typesafe.akka"             %% "akka-stream"              % akkaVer,
  "com.typesafe.akka"             %% "akka-http"                % "10.1.7",
  "com.typesafe.play"             %% "play-json"                % "2.6.13"
)