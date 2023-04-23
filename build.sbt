lazy val sonatypePublic = "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"
lazy val sonatypeReleases = "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
lazy val sonatypeSnapshots = "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"
lazy val circeVersion = "0.14.1"

resolvers ++= Seq(Resolver.mavenLocal, sonatypeReleases, sonatypeSnapshots, Resolver.mavenCentral)

lazy val appkit = "org.ergoplatform" %% "ergo-appkit" % "5.0.0"

libraryDependencies ++= Seq(
  appkit, (appkit % Test).classifier("tests").classifier("tests-sources"),
  "com.github.scopt" %% "scopt" % "4.0.1",
  "com.squareup.okhttp3" % "mockwebserver" % "3.12.0",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.1" % "test",
  "com.typesafe" % "config" % "1.4.1",
  "org.scalatest" %% "scalatest" % "3.2.11" % "test",
  "org.scalacheck" %% "scalacheck" % "1.15.4" % "test",
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test
)

name := "contract"
ThisBuild / scalaVersion := "2.12.7"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "rosen.bridge"
ThisBuild / organizationName := "rosen-bridge"
ThisBuild / publishMavenStyle := true
Test / publishArtifact := false

assembly / assemblyJarName := s"${name.value}-${organizationName.value}-${version.value}.jar"

