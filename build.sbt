lazy val root = (project in file("."))
  .settings(
    name := "github-ranking",
    scalaVersion := "2.12.8",
    version := "0.1.0",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"   % "10.1.9",
      "com.typesafe.akka" %% "akka-stream" % "2.5.23",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.9",
      "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.23" % "test",
      "com.typesafe.akka" %% "akka-http-testkit" % "10.1.10" % "test",
      "org.scalatest" %% "scalatest" % "3.0.8" % "test"
    ),
    assemblyJarName in assembly := "github-ranking.jar",
    mainClass in assembly := Some("com.shivashriti.ranking.Server")
  )