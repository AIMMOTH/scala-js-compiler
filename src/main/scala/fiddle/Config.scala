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
  //  protected val config = ConfigFactory.load().getConfig("fiddle")
  // read the generated version data
  protected val versionProps = new Properties()
  //  versionProps.load(getClass.getResourceAsStream("/version.properties"))
  versionProps.setProperty("version", "1.0.0-SNAPSHOT")
  versionProps.setProperty("scalaVersion", "2.11.8")
  versionProps.setProperty("scalaJSVersion", "0.6.9")
  versionProps.setProperty("aceVersion", "1.2.2")

  val interface = "0.0.0.0" // config.getString("interface")
  val port = 8080 // config.getInt("port")
  val analyticsID = "UA-74405486-1" // config.getString("analyticsID")
  val helpUrl = "https://github.com/ochrons/scala-js-fiddle/blob/embeddable/UsersGuide.md" // config.getString("helpUrl")

  val clientFiles = List("/client-fastopt.js") // config.getStringList("clientFiles").asScala

  val extLibs =
    //    config.getConfig("extLibs").entrySet().asScala
    Map("dom" -> "org.scala-js %%% scalajs-dom % 0.9.0",
      "scalatags" -> "com.lihaoyi %%% scalatags % 0.5.4",
      "async" -> "org.scala-lang.modules %% scala-async % 0.9.5")
  //  .map { entry =>
  //    entry.getKey -> entry.getValue.unwrapped().asInstanceOf[String]
  //  }.toMap

  val environments = Map("default" -> List("dom", "scalatags", "async"))
  //    config.getConfig("environments").entrySet().asScala.map { entry =>
  //    entry.getKey -> entry.getValue.unwrapped().asInstanceOf[java.util.List[String]].asScala.toList
  //  }.toMap

  val extJS = List("https://ajax.googleapis.com/ajax/libs/jquery/2.2.2/jquery.min.js") // config.getStringList("extJS").asScala
  val extCSS = List() // config.getStringList("extCSS").asScala

  val libCache = "target/extlibs" // config.getString("libCache")

  val templates = Map(
    "default" -> Template(
      """
        import scalatags.JsDom.all._
        import org.scalajs.dom
        import fiddle.Fiddle, Fiddle.println
        import scalajs.js
        object ScalaFiddle extends js.JSApp {
          def main() = {
      """,
      """
          }
        }
      """),
    "imports" -> Template(
      """
import scalatags.JsDom.all._
import org.scalajs.dom
import fiddle.Fiddle, Fiddle.println
import scalajs.js
""",
      ""),
    "main" -> Template(
      """
import scalatags.JsDom.all._
import org.scalajs.dom
import fiddle.Fiddle, Fiddle.println
import scalajs.js
object ScalaFiddle extends js.JSApp {
""",
      """
}
"""),
    "repl" -> Template(
      """
import scala.reflect.ClassTag
import scalatags.JsDom.all._
import org.scalajs.dom
import fiddle.Fiddle, Fiddle.println
import scalajs.js
object ScalaFiddle extends js.JSApp {
  def printResult[T: ClassTag](r: T): Unit = {
    val tpe = implicitly[ClassTag[T]].runtimeClass.getSimpleName
    println(s"res: $tpe = ${r.toString}")
  }

  def main() = {
    printResult(repl())
  }

  def repl() = {
""",
      """
  }
}
"""),
    "raw" -> Template("", ""))
  //    config.getConfigList("templates").asScala.map { co =>
  //    co.getString("name") -> Template(co.getString("pre"), co.getString("post"))
  //  }.toMap

  val httpHeaders: List[HttpHeader] = List()
//  config.getConfig("httpHeaders").entrySet().asScala.map { entry =>
//    RawHeader(entry.getKey, entry.getValue.unwrapped().asInstanceOf[String])
//  }.toList

  val version = versionProps.getProperty("version")
  val scalaVersion = versionProps.getProperty("scalaVersion")
  val scalaMainVersion = scalaVersion.split('.').take(2).mkString(".")
  val scalaJSVersion = versionProps.getProperty("scalaJSVersion")
  val scalaJSMainVersion = scalaJSVersion.split('.').take(2).mkString(".")
  val aceVersion = versionProps.getProperty("aceVersion")
}

