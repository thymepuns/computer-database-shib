play.Project.playScalaSettings

name := "computer-database-shib"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.2"

libraryDependencies in Global ++= Seq(
  "com.typesafe.play"       %% "play-jdbc"                   % "2.2.1",
//  "com.typesafe.play"       %% "anorm"                       % "2.2.1",
  "com.typesafe.play"       %% "play-cache"                  % "2.2.1",
  "log4j" % "log4j" % "1.2.16",
  "org.hibernate" % "hibernate-core" % "4.2.7.Final",
  "org.hibernate" % "hibernate-c3p0" % "4.2.7.Final",
  "org.hibernate.java-persistence" % "jpa-api" % "2.0-cr-1" ,
  "com.mchange" % "c3p0" % "0.9.2.1",
  "org.scala-lang" % "scala-reflect" % "2.10.2"
)

resolvers in Global ++= Seq(
 "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

scalacOptions in Global += "-feature"

lazy val root = project.in(file(".")).aggregate(shib).dependsOn(shib)

lazy val shib = project.in(file("shib"))


