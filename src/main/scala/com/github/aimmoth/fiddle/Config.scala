package com.github.aimmoth.fiddle

import java.util.Properties

object Config {
  protected val versionProps = new Properties()
  versionProps.setProperty("scalaVersion", "2.11.8")
  versionProps.setProperty("scalaJSVersion", "0.6.10")

  val environments = Map("default" -> List("dom", "scalatags", "async"))

  val libCache = "target/extlibs"

  val scalaVersion = versionProps.getProperty("scalaVersion")
  val scalaMainVersion = scalaVersion.split('.').take(2).mkString(".")
  val scalaJSVersion = versionProps.getProperty("scalaJSVersion")
  val scalaJSMainVersion = scalaJSVersion.split('.').take(2).mkString(".")
  val aceVersion = versionProps.getProperty("aceVersion")
}

