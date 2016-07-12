package fiddle

import java.security.MessageDigest

//import akka.util.ByteString

import scala.collection.concurrent.TrieMap
import scala.reflect.io.Streamable
import scala.util.Try

object Static {
  val aceFiles = Seq(
    s"/META-INF/resources/webjars/ace/${Config.aceVersion}/src-min/ace.js",
    s"/META-INF/resources/webjars/ace/${Config.aceVersion}/src-min/ext-language_tools.js",
    s"/META-INF/resources/webjars/ace/${Config.aceVersion}/src-min/ext-static_highlight.js",
    s"/META-INF/resources/webjars/ace/${Config.aceVersion}/src-min/mode-scala.js",
    s"/META-INF/resources/webjars/ace/${Config.aceVersion}/src-min/theme-eclipse.js",
    s"/META-INF/resources/webjars/ace/${Config.aceVersion}/src-min/theme-tomorrow_night_eighties.js"
  )

  val cssFiles = Seq(
    "/META-INF/resources/webjars/normalize.css/2.1.3/normalize.css",
    "/common.css"
  )

  val buttons = Seq(
    ("run", "Ctrl/Cmd-Enter to run,\nShift-Ctrl/Cmd-Enter to run optimized"),
    ("reset", "Reset"),
    ("share", "Share"),
    ("help", "Help")
  )

  // store concatenated and hashed resource blobs
  val cache = TrieMap.empty[Seq[String], (String, Array[Byte])]

  final val layoutRE = """([vh])(\d\d)""".r

  def concatHash(resources: Seq[String], glueStr: String): (String, Array[Byte]) = {
    val hash = MessageDigest.getInstance("MD5")
    // files need a bit of glue between them to work properly in concatenated form
    val glue = glueStr.getBytes
    // read all resources and calculate both hash and concatenated string
    val data = resources.map { res =>
      val stream = getClass.getResourceAsStream(res)
      val data = Streamable.bytes(stream) ++ glue
      hash.update(data)
      data
    }.reduceLeft(_ ++ _)
    (hash.digest().map("%02x".format(_)).mkString, data)
  }

  def joinResources(resources: Seq[String], extension: String, glueStr: String): String = {
    cache.getOrElseUpdate(resources, concatHash(resources, glueStr))._1 + extension
  }

  def fetchResource(hash: String): Option[Array[Byte]] = {
    cache.values.find(_._1 == hash).map(_._2)
  }
}
