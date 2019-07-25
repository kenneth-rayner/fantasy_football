import play.sbt.routes.RoutesKeys

name := """fantasy_football"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.0"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test


routesGenerator := InjectedRoutesGenerator

libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.18.1-play27"

RoutesKeys.routesImport += "models.CardId"