package io.cenet.compiler

import java.util.Properties

//import akka.http.scaladsl.model.HttpHeader
//import akka.http.scaladsl.model.headers.RawHeader
//import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

//case class Template(pre: String, post: String) {
//  def fullSource(src: String) = pre + src + post
//}

object VirtualConfig {
//  protected val config = ConfigFactory.load().getConfig("fiddle")
  // read the generated version data
//  protected val versionProps = new Properties()
//  versionProps.load(getClass.getResourceAsStream("/version.properties"))

//  val interface = "0.0.0.0"
//  val port = 8080
//  val analyticsID = "UA-74405486-1"
//  val helpUrl = "https://github.com/ochrons/scala-js-fiddle/blob/embeddable/UsersGuide.md"

  val clientFiles = List("/client-fastopt.js")

//  val extLibs = Map(
//    "dom" -> "org.scala-js %%% scalajs-dom % 0.9.0",
//    "scalatags" -> "com.lihaoyi %%% scalatags % 0.5.4"
//    ,
//    "async" -> "org.scala-lang.modules %% scala-async % 0.9.5"
//    ).map { entry =>
//    entry._1 -> entry._2
//  }.toMap

//  val environments = config.getConfig("environments").entrySet().asScala.map { entry =>
//    entry.getKey -> entry.getValue.unwrapped().asInstanceOf[java.util.List[String]].asScala.toList
//  }.toMap
  val environments = Map("default" -> List("dom", "scalatags", "async"))

//  val extJS = List("https://ajax.googleapis.com/ajax/libs/jquery/2.2.2/jquery.min.js")
//  val extCSS = Nil

  val libCache = "target/extlibs"

//  val templates = config.getConfigList("templates").asScala.map { co =>
//    co.getString("name") -> Template(co.getString("pre"), co.getString("post"))
//  }.toMap

//  val httpHeaders: List[HttpHeader] = config.getConfig("httpHeaders").entrySet().asScala.map { entry =>
//    RawHeader(entry.getKey, entry.getValue.unwrapped().asInstanceOf[String])
//  }.toList

//  val version = versionProps.getProperty("version")
  val scalaVersion = "2.11.8"
  val scalaMainVersion = scalaVersion.split('.').take(2).mkString(".")
  val scalaJSVersion = "0.6.8"
  val scalaJSMainVersion = scalaJSVersion.split('.').take(2).mkString(".")
//  val aceVersion = versionProps.getProperty("aceVersion")
}