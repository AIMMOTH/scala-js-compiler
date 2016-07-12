package fiddle

import java.util.Properties

//import akka.http.scaladsl.model.HttpHeader
//import akka.http.scaladsl.model.headers.RawHeader
//import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

case class Template(pre: String, post: String) {
  def fullSource(src: String) = pre + src + post
}

object Config {
  protected val versionProps = new Properties()
  versionProps.setProperty("version", "1.0.0-SNAPSHOT")
  versionProps.setProperty("scalaVersion", "2.11.8")
  versionProps.setProperty("scalaJSVersion", "0.6.9")
  versionProps.setProperty("aceVersion", "1.2.2")

  val interface = "0.0.0.0"
  val port = 8080
  val analyticsID = "UA-74405486-1"
  val helpUrl = "https://github.com/ochrons/scala-js-fiddle/blob/embeddable/UsersGuide.md"

  val clientFiles = List("/client-fastopt.js")

  val extLibs =
    Map(
//        "dom" -> "org.scala-js %%% scalajs-dom % 0.9.0",
//      "scalatags" -> "com.lihaoyi %%% scalatags % 0.5.4",
//      "async" -> "org.scala-lang.modules %% scala-async % 0.9.5"
      )

  val environments = Map("default" -> List("dom", "scalatags", "async"))

  val extJS = List("https://ajax.googleapis.com/ajax/libs/jquery/2.2.2/jquery.min.js") // config.getStringList("extJS").asScala
  val extCSS = List()

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

  val version = versionProps.getProperty("version")
  val scalaVersion = versionProps.getProperty("scalaVersion")
  val scalaMainVersion = scalaVersion.split('.').take(2).mkString(".")
  val scalaJSVersion = versionProps.getProperty("scalaJSVersion")
  val scalaJSMainVersion = scalaJSVersion.split('.').take(2).mkString(".")
  val aceVersion = versionProps.getProperty("aceVersion")
}

