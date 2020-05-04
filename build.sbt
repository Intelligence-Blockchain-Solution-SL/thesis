organization := "es.ibs"

name := "thesis"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.11"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-explaintypes", "-encoding", "utf-8",
  "-Xlint", "-Xfatal-warnings")

lazy val akkaVer = "2.6.4"
libraryDependencies ++= Seq(
  // java
  "org.slf4j"                     %  "jul-to-slf4j"             % "1.7.30",
  "ch.qos.logback"                %  "logback-classic"          % "1.2.3",
  "com.github.maricn"             %  "logback-slack-appender"   % "1.4.0",
  "com.google.inject.extensions"  %  "guice-assistedinject"     % "4.2.2",
  "org.postgresql"                %  "postgresql"               % "42.2.12",
  "com.zaxxer"                    %  "HikariCP"                 % "3.4.3",
  "org.apache.poi"                %  "poi-ooxml"                % "4.1.2",
  // scala
  "com.typesafe.scala-logging"    %% "scala-logging"            % "3.9.2",
  "net.codingwell"                %% "scala-guice"              % "4.2.6",
  "com.typesafe.akka"             %% "akka-actor"               % akkaVer,
  "com.typesafe.akka"             %% "akka-slf4j"               % akkaVer,
  "com.typesafe.akka"             %% "akka-stream"              % akkaVer,
  "com.typesafe.akka"             %% "akka-http"                % "10.1.11",
  "com.typesafe.play"             %% "play-json"                % "2.8.1"
)