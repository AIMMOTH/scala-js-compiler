package com.github.aimmoth.scalajs.compiler

import com.github.scalafiddle.{Classpath, CompileActor, Optimizer}
import org.scalajs.core.tools.logging.{Level => JsLevel}

import java.io.InputStream
import java.util.logging.Logger
import java.util.zip.{ZipEntry, ZipFile}
import scala.io.Source
import scala.language.postfixOps
import scala.reflect.io.VirtualFile

case class ScalaJsFile(filename: String = "ScalaFiddle", source: String)

object ScalaJsCompiler {
  def apply() = new ScalaJsCompiler
}

class ScalaJsCompiler {

  val log = Logger.getLogger(getClass.getName)
  var classpath : Classpath = null

  type ErrorLog = String
  type JavaScriptSource = String
  type CompilationResult = Either[ErrorLog, JavaScriptSource]

  def compileJarWithScalaJsSource(jarWithSource: ZipFile, optimizer: Optimizer, charsetName: String = "UTF-8") = {
    jarWithSource.entries match {
      case entries => 
        (new Iterator[ZipEntry] {
          def next = entries.nextElement
          def hasNext = entries.hasMoreElements
        }).filter(entry => entry.getName.endsWith(".scala"))
        .map(entry => {
          Option(jarWithSource.getInputStream(entry))
            .map(Source.fromInputStream(_))
            .map(_.mkString)
            .filter(_.length > 0)
            .map(ScalaJsFile(entry.getName, _))
            .map(compileScalaJsString(_, optimizer, charsetName))
        })
        .flatten
    }
  }

  def compileScalaJsString(source: ScalaJsFile, optimizer: Optimizer, charsetName: String = "UTF-8") = {
    compileScalaJsStrings(List(source), optimizer)
  }
  def compileScalaJsUtf8StringFast(source: ScalaJsFile) = compileScalaJsStrings(List(source), Optimizer.Fast)
  def compileScalaJsUtf8StringFull(source: ScalaJsFile) = compileScalaJsStrings(List(source), Optimizer.Full)

  /**
   * String with Scala JS code
   */
  def compileScalaJsStrings(sources: List[ScalaJsFile], optimizer: Optimizer, charsetName: String = "UTF-8", minLevel: JsLevel = JsLevel.Info): CompilationResult = {

    if (classpath == null) {
      throw new RuntimeException("Run init to load classpath files first!")
    }

    /**
     * Converts a bunch of bytes into Scalac's weird VirtualFile class
     */
    def makeFile(filename: String, src: Array[Byte]) = {
      val singleFile = new VirtualFile(filename)
      val output = singleFile.output
      output.write(src)
      output.close()
      singleFile
    }

    val files = sources.map(s => makeFile(s.filename, s.source.getBytes(charsetName)))

    new CompileActor(classpath, files, optimizer, minLevel).doCompile match {
      case cr if cr.jsCode.isDefined =>
        Right(cr.jsCode.get)
      case cr => {
        val text = cr.log
    	  Left(text)
      }
    }
  }

  /**
   * Call this method first to load all libraries. Loader could be reading local files:
   * <pre>
   *   // Read all jars file paths on classpath
   *   val classpath = System.getProperty("java.class.path").split(";")
   *   val loader : (String => InputStream) = (jarFilename: String) => {
   *     classpath.find(s => s.endsWith(jarFilename)) match {
   *       case Some(found) => {
   *         println("Found on classpath:" + found)
   *         new FileInputStream(new File(found))
   *       }
   *       case None => throw new FileNotFoundException(jarFilename)
   *     }
   *   }
   * </pre>
   *
   * @param loader In a Servlet context it could be <pre>(jarFile:String) => ServletContext.getResourceAsStream(jarFile)</pre>
   * @param relativeJarPath Use "" if running locally, or "/WEB-INF/lib/" when loading from Servlet context
   * @param additionalLibs Any provided jar from a dependency. For instance "scalajs-dom_sjs0.6_2.11-0.9.8.jar"
   * @param baseLibs Default is Seq("scala-library-2.11.12.jar", "scala-reflect-2.11.12.jar", "scalajs-library_2.11-0.6.33.jar")
   */
  def init(loader: (String) => InputStream, relativeJarPath: String = "", additionalLibs : Set[String] = Set(), baseLibs: Seq[String] = Seq("scala-library-2.11.12.jar", "scala-reflect-2.11.12.jar", "scalajs-library_2.11-0.6.33.jar")) = {
    classpath = Classpath(loader, relativeJarPath, baseLibs, additionalLibs)
    this
  }
}