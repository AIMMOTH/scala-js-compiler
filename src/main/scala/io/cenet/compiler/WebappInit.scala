package io.cenet.compiler

import java.util.logging.Logger
import scala.collection.JavaConverters.asScalaSetConverter
import com.google.common.io.ByteStreams
import javax.servlet.ServletContextEvent
import javax.servlet.ServletContextListener
import java.util.zip.ZipFile
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
import scala.reflect.io.VirtualDirectory
import java.io.File

class WebappInit {

  val log = Logger.getLogger(classOf[WebappInit].getName())

//  override def contextDestroyed(context :ServletContextEvent) = {
//    log.info("kthxbye");
//  }

//  override def contextInitialized(contextEvent : ServletContextEvent) {
//  override def doPost(request: HttpServletRequest, response: HttpServletResponse) = doGet(request, response)
//  override def doGet (request: HttpServletRequest, response: HttpServletResponse) = {

    log.info("Loading files ...")
//    getClass.getResourceAsStream("scalajs-library_2.11-0.6.8.jar")

//    contextEvent.getServletContext match {
//      case context =>
//        val f = context.getResourcePaths("/WEB-INF/classes/libs").asInstanceOf[java.util.Set[String]]
        val file = new File(".")
    log.fine("File:" + file.getAbsoluteFile);
        val f = file.list()
        log.fine("files:" + f)
//        JarFiles.files = f.asScala.map(_.substring("/WEB-INF/classes".length)).toSeq

        def recursive(resource : String) : Set[String] =
          if (resource.endsWith(".class") || resource.endsWith(".sjsir")) {
            Set(resource.substring("/WEB-INF/classes/libs".length))
          } else {
//            val dirs = context.getResourcePaths(resource).asInstanceOf[java.util.Set[String]]
            val dirs = Option(new File(resource).list())
            dirs.getOrElse(Array()).map(recursive).toSet.flatten
          }
        def toBytes(f : Seq[String]) = { f.map{
            case file =>
//              log.info("rad 43 file:" + file)
//              (file, ByteStreams.toByteArray(getClass.getResourceAsStream(file)))
              (file, file.getBytes)
        }}

        def toVirtual(f : Seq[(String, Array[Byte])]) = { f.map{
            case (file, b) =>
              val tokens = file.split("/")
              val dir = new VirtualDirectory(tokens.head, None)
              def r(parent : VirtualDirectory, folders : Array[String]) : VirtualDirectory = {
                if (folders.isEmpty) {
                  parent
                } else {
                  val p = parent.subdirectoryNamed(folders.head).asInstanceOf[VirtualDirectory]
                  r(p, folders.tail)
                }
              }
              val folder = r(dir, tokens.dropRight(1).tail)
          //    val dirs = for (t <- tokens.tail.dropRight(1)) yield
          //      dir.subdirectoryNamed(t).asInstanceOf[VirtualDirectory]

              val f = folder.fileNamed(tokens.last)
              if (f.name == "Object.class")
                log.info(s"${f.name} in ${f.canonicalPath}")
              val o = f.bufferedOutput
              o.write(b)
              o.close()

              dir
          }.seq
        }
        val files = f.map(recursive).toSeq.flatten
        log.info("ALL FILES:" + files)
        JarFiles.jarBytes = toBytes(files)
        JarFiles.jarFiles = toVirtual(JarFiles.jarBytes)

//        val source = context.getResourcePaths("/WEB-INF/classes/example/scalajs").asInstanceOf[java.util.Set[String]]
        val source = new File("/WEB-INF/classes/example/scalajs").list()
        JarFiles.sourceFiles = toBytes(source.map(_.substring("/WEB-INF/classes".length)).toSeq)

        log.info("Done loading bytes.")
//        log.info("all files:" + JarFiles.files.mkString)
//        log.info("all source:" + JarFiles.source.mkString)
//    }
//  }
}

object JarFiles {

  val log = Logger.getLogger("JarFiles")

  var jarBytes : Seq[(String, Array[Byte])] = null
  var jarFiles : Seq[VirtualDirectory] = null
  var sourceFiles : Seq[(String, Array[Byte])] = null

}