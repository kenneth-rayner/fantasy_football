import play.sbt.routes.RoutesKeys

name := """fantasy_football"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.0"

libraryDependencies += guice

routesGenerator := InjectedRoutesGenerator

libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.18.1-play27"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.8"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.8" % Test
libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % Test

RoutesKeys.routesImport += "models.CardId"
