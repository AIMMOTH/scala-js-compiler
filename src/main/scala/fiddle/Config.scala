package fiddle

import java.util.Properties

object Config {
  protected val versionProps = new Properties()
  versionProps.setProperty("version", "1.0.0-SNAPSHOT")
  versionProps.setProperty("scalaVersion", "2.11.8")
  versionProps.setProperty("scalaJSVersion", "0.6.9")
  versionProps.setProperty("aceVersion", "1.2.2")

  val interface = "0.0.0.0"
  val port = 8080

  val clientFiles = List("/client-fastopt.js")

  val environments = Map("default" -> List("dom", "scalatags", "async"))

  val extJS = List("https://ajax.googleapis.com/ajax/libs/jquery/2.2.2/jquery.min.js")
  val extCSS = List()

  val libCache = "target/extlibs"

  val version = versionProps.getProperty("version")
  val scalaVersion = versionProps.getProperty("scalaVersion")
  val scalaMainVersion = scalaVersion.split('.').take(2).mkString(".")
  val scalaJSVersion = versionProps.getProperty("scalaJSVersion")
  val scalaJSMainVersion = scalaJSVersion.split('.').take(2).mkString(".")
  val aceVersion = versionProps.getProperty("aceVersion")
}

