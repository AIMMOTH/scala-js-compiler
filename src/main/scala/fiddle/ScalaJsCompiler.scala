package fiddle

import scala.language.postfixOps
import scala.reflect.io.VirtualFile

import org.slf4j.LoggerFactory
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import scala.io.Source
import java.util.zip.ZipInputStream

class ScalaJsCompiler {

  val log = LoggerFactory.getLogger(getClass)

    def compileJarWithScalaJsSource(context : ClassLoader, jarWithSource: ZipFile, optimizer: Optimizer, relativeJarPath: String): String = {
    jarWithSource.entries match {
      case entries => 
        (new Iterator[ZipEntry] {
          def next = entries.nextElement
          def hasNext = entries.hasMoreElements
        }).filter(entry => entry.getName.endsWith(".scala"))
        .map(entry => Source.fromInputStream(jarWithSource.getInputStream(entry)).mkString)
        .mkString match {
          case source =>
            compileScalaJsString(context, source, optimizer, relativeJarPath)
        }
    }
  }
  
  def compileScalaJsString(context : ClassLoader, source: String, optimizer: Optimizer, relativeJarPath: String): String = {
    compileScalaJsStrings(context, List(source), optimizer, relativeJarPath)
  }
  
  /**
   * String with Scala JS code
   */
  def compileScalaJsStrings(context : ClassLoader, source: List[String], optimizer: Optimizer, relativeJarPath: String): String = {
    /**
     * Converts a bunch of bytes into Scalac's weird VirtualFile class
     */
    def makeFile(src: Array[Byte]) = {
      val singleFile = new VirtualFile("ScalaFiddle.scala")
      val output = singleFile.output
      output.write(src)
      output.close()
      singleFile
    }

    val files = source.map(s => makeFile(s.getBytes("UTF-8")))

    val actor = new CompileActor(Classpath(context, relativeJarPath), "scalatags", files, optimizer)
    actor.doCompile match {
      case cr if cr.jsCode.isDefined =>
        cr.jsCode.get
      case cr =>
        throw new Exception(cr.log)
    }
  }

}