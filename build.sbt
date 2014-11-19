name := """jackhammer-web"""

version := "1.0-SNAPSHOT"

lazy val dselang = project in file("dse-lang")

lazy val root = (project in file(".")).enablePlugins(PlayScala).dependsOn(dselang)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws
)
