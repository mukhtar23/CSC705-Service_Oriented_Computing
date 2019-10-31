name := """soc_project3"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.0"

libraryDependencies += guice
libraryDependencies += "com.github.galigator.openllet" % "openllet-jena" % "2.6.4"