name := "aws-lambda-scala"
description  := "Library for creating Lamdbda functions in Scala"
organization := "com.pharmpress"

scalaVersion in ThisBuild := "2.12.11"

fork in Test := true

javaOptions in Test ++= Seq("-Dfile.encoding=UTF-8")

// Manually add the version when creating snapshots or while testing locally
lazy val versionNumber: String = "1"

val codeBuild_Number: String = sys.env.getOrElse("CODEBUILD_BUILD_NUMBER", versionNumber)
version in ThisBuild := s"0.${codeBuild_Number}.0"  // For snapshots, add suffix: 0.1.0-SNAPSHOT

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-events" % "2.2.2",
  "io.circe" %% "circe-core" % "0.12.3",
  "io.circe" %% "circe-generic" % "0.12.3",
  "io.circe" %% "circe-parser" % "0.12.3",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "com.amazonaws" % "aws-java-sdk-lambda" % "1.11.815",
  "org.scalatest" %% "scalatest" % "3.1.0" % "test",
  "org.mockito" %% "mockito-scala" % "1.10.0" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"
)


// https://github.com/arktekk/sbt-aether-deploy
overridePublishBothSettings

publishTo := {
  val codeArtifactUrl = "https://pharmpress-506600287196.d.codeartifact.eu-west-1.amazonaws.com/maven/"
  val snapshots = "snapshots"
  val releases = "releases"

  if ((version in ThisBuild).value.endsWith("SNAPSHOT")) {
    Some(s"pharmpress--$snapshots" at codeArtifactUrl + s"$snapshots")
  } else {
    Some(s"pharmpress--$releases"  at codeArtifactUrl + s"$releases")
  }
}

// First export a CodeArtifact authorization token for authorization to the repository
lazy val codeArtifact_Token: String = System.getenv("CODEARTIFACT_AUTH_TOKEN")

// Credentials with a reference to the CODEARTIFACT_TOKEN so that Maven passes the token in HTTP requests
credentials += Credentials(
  "",
  "pharmpress-506600287196.d.codeartifact.eu-west-1.amazonaws.com",
  "aws",
  s"$codeArtifact_Token"
)

lazy val compilerOptions = Seq(
  "-unchecked",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-deprecation",
  "-encoding",
  "utf8"
)
