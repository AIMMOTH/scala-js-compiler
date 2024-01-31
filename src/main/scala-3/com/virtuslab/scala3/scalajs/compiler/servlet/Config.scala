package com.virtuslab.scala3.scalajs.compiler

object Config {

  val environments: Map[String, List[String]] = Map[String, List[String]]("default" -> List("dom", "scalatags", "async"))

  val scala2LibraryVersion = "2.13.10"
  val scalaVersion = "3.3.1"
  val scalaMainVersion = scalaVersion.split('.').take(2).mkString(".")
  val scalaJSVersion = "1.12.0"
  val scalaJSMainVersion = scalaJSVersion.split('.').take(2).mkString(".")
}

