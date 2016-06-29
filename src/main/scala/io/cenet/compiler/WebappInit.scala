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
import java.nio.file.Paths
import java.nio.file.Files

class WebappInit {

  val log = Logger.getLogger(classOf[WebappInit].getName())

  try {
    log.info("Loading files ...")
    val file = new File("WEB-INF/classes/libs")
    log.fine("File:" + file.getAbsoluteFile);
    val f = file.listFiles()
    log.fine("files:" + f)

    def recursive(resource: File): Set[File] =
      if (resource.getName.endsWith(".class") || resource.getName.endsWith(".sjsir")) {
        Set(resource)
      } else {
        resource.listFiles() match {
          case null => Set()
          case list => list.map(recursive).toSet.flatten
        }
      }
    def toBytes(f: Seq[File]) = {
      f.map {
        case file =>
          (file, Files.readAllBytes(Paths.get(file.getAbsolutePath)))
      }
    }

    def toVirtual(f: Seq[(File, Array[Byte])]) = {
      f.map {
        case (file, b) =>
          val tokens = file.getPath.split("\\\\").drop(3)
          val dir = new VirtualDirectory(tokens.head, None)
          def r(parent: VirtualDirectory, folders: Array[String]): VirtualDirectory = {
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
            log.info(s"${f.name} in ${f.canonicalPath} (${b.length}b)")
          val o = f.bufferedOutput
          o.write(b)
          o.close()

          dir
      }.seq
    }
    val files = f.map(recursive).toSeq.flatten
    JarFiles.jarBytes = toBytes(files)
    JarFiles.jarFiles = toVirtual(JarFiles.jarBytes)

    log.info("Done loading bytes.")
  } catch {
    case e: Throwable =>
      e.printStackTrace()
  }
}

object JarFiles {

  val log = Logger.getLogger("JarFiles")

  var jarBytes: Seq[(File, Array[Byte])] = null
  var jarFiles: Seq[VirtualDirectory] = null
}