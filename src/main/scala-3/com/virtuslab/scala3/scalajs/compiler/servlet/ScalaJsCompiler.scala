package com.virtuslab.scala3.scalajs.compiler

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import scala.io.Source
import scala.language.postfixOps
import dotty.tools.io.VirtualFile

import java.io.InputStream
import java.util.logging.Logger
import javax.servlet.ServletContext

case class ScalaJsFile(filename: String = "ScalaFiddle", source: String)

object ScalaJsCompiler {
  def apply() = new ScalaJsCompiler
}

class ScalaJsCompiler {

  val log = Logger.getLogger(getClass.getName)

  def compileJarWithScalaJsSource(classpath: Classpath, jarWithSource: ZipFile, optimizer: Optimizer): String = {
    jarWithSource.entries match {
      case entries => 
        (new Iterator[ZipEntry] {
          def next = entries.nextElement
          def hasNext = entries.hasMoreElements
        }).filter(entry => entry.getName.endsWith(".scala"))
        .map(entry => Source.fromInputStream(jarWithSource.getInputStream(entry)).mkString)
        .mkString match {
          case source =>
            compileScalaJsString(classpath, source, optimizer)
        }
    }
  }
  
  def compileScalaJsString(classpath: Classpath, source: String, optimizer: Optimizer): String = {
    compileScalaJsStrings(classpath, List(source), optimizer)
  }
  
  /**
   * String with Scala JS code
   */
  def compileScalaJsStrings(classpath: Classpath, sources: List[String], optimizer: Optimizer): String = {
    /**
     * Converts a bunch of bytes into Scalac's weird VirtualFile class
     */
    def makeFile(src: Array[Byte]) = {
      log.info(s"Creating virtual file from byte size ${src.length}")
      val singleFile = new VirtualFile("ScalaFiddle.scala")
      val output = singleFile.output
      output.write(src)
      output.close()
      singleFile
    }

    log.info(s"Files to compile ${sources.size}")
    val files = sources.map(s => makeFile(s.getBytes("UTF-8")))
    
    if (classpath == null) {
      throw new RuntimeException("Run init to load classpath files first!")
    }

    new CompileActor(classpath, "scalatags", files, optimizer).doCompile match {
      case cr if cr.jsCode.isDefined =>
        cr.jsCode.get
      case cr =>
        val text = cr.log
        log.warning("ScalaJSCompiler warning!")
        log.warning(text)
        log.warning(s"Compiler result object: $cr")
        throw new Exception(text)
    }
  }

  /**
   * Use this method to create Classpath. It takes time to created it so make sure to keep it.
   */
  def init(loader: (String => InputStream), relativeJarPath: String, libs : Set[String] = Set()): Classpath = {
    Classpath(loader, relativeJarPath, libs)
  }
}