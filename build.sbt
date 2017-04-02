organization := "mate1"

name := "hyper-queue"

version := "0.0.1"

scalaVersion := "2.11.8"

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.4.17"
  val sprayV = "1.3.3"
  val sprayJsonV = "1.3.2"
  Seq(
    "ch.qos.logback"              % "logback-classic" % "1.1.7",
    "com.typesafe.akka"          %% "akka-actor"      % akkaV,
    "com.typesafe.akka"          %% "akka-testkit"    % akkaV       % "test",
    "com.typesafe.akka"          %% "akka-slf4j"      % akkaV,
    "com.typesafe.scala-logging" %% "scala-logging"   % "3.5.0",
    "io.spray"                   %% "spray-can"       % sprayV,
    "io.spray"                   %% "spray-routing"   % sprayV,
    "io.spray"                   %% "spray-client"    % sprayV,
    "io.spray"                   %% "spray-json"      % sprayJsonV,
    "io.spray"                   %% "spray-testkit"   % sprayV      % "test"
  )
}

assemblyMergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList(ps @ _*) if ps.last.endsWith(".class") =>
      MergeStrategy.first
    case PathList(ps @ _*) if ps.last.endsWith(".file") =>
      MergeStrategy.first
    case x => old(x)
  }
}

assemblyJarName in assembly := "hyper-queue.jar"
