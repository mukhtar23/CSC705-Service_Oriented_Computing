name := """soc_project4"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.13.0"

resolvers += "public-jboss" at "http://repository.jboss.org/nexus/content/groups/public-jboss/"

libraryDependencies += guice
libraryDependencies += "com.github.galigator.openllet" % "openllet-jena" % "2.6.4"
libraryDependencies ++= Seq(
"org.drools" % "drools-core" % "7.28.0.Final",
"org.drools" % "drools-compiler" % "7.28.0.Final"
)