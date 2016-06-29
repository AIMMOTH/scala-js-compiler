package fiddle

import java.util.Properties

import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

case class Template(pre: String, post: String) {
  def fullSource(src: String) = pre + src + post
}

object Config {
  protected val config = ConfigFactory.load().getConfig("fiddle")
  // read the generated version data
  protected val versionProps = new Properties()
  versionProps.load(getClass.getResourceAsStream("/version.properties"))

  val interface = config.getString("interface")
  val port = config.getInt("port")
  val analyticsID = config.getString("analyticsID")
  val helpUrl = config.getString("helpUrl")

  val clientFiles = config.getStringList("clientFiles").asScala

  val extLibs = config.getConfig("extLibs").entrySet().asScala.map { entry =>
    entry.getKey -> entry.getValue.unwrapped().asInstanceOf[String]
  }.toMap

  val environments = config.getConfig("environments").entrySet().asScala.map { entry =>
    entry.getKey -> entry.getValue.unwrapped().asInstanceOf[java.util.List[String]].asScala.toList
  }.toMap

  val extJS = config.getStringList("extJS").asScala
  val extCSS = config.getStringList("extCSS").asScala

  val libCache = config.getString("libCache")

  val templates = config.getConfigList("templates").asScala.map { co =>
    co.getString("name") -> Template(co.getString("pre"), co.getString("post"))
  }.toMap

  val httpHeaders: List[HttpHeader] = config.getConfig("httpHeaders").entrySet().asScala.map { entry =>
    RawHeader(entry.getKey, entry.getValue.unwrapped().asInstanceOf[String])
  }.toList

  val version = versionProps.getProperty("version")
  val scalaVersion = versionProps.getProperty("scalaVersion")
  val scalaMainVersion = scalaVersion.split('.').take(2).mkString(".")
  val scalaJSVersion = versionProps.getProperty("scalaJSVersion")
  val scalaJSMainVersion = scalaJSVersion.split('.').take(2).mkString(".")
  val aceVersion = versionProps.getProperty("aceVersion")
}

