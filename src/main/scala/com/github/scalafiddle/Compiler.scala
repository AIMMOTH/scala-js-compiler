package com.github.scalafiddle

import java.io.{PrintWriter, Writer}
import org.scalajs.core.tools.io._
import org.scalajs.core.tools.linker.Linker
import org.scalajs.core.tools.logging.{Level => JsLevel, Logger => JsLogger}
import org.scalajs.core.tools.sem.Semantics

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.io
import scala.tools.nsc
import scala.tools.nsc.Settings
import scala.tools.nsc.backend.JavaPlatform
import scala.tools.nsc.interactive.Response
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.reporters.{ConsoleReporter, StoreReporter}
import scala.tools.nsc.typechecker.Analyzer
import scala.tools.nsc.util.ClassPath.JavaContext
import scala.tools.nsc.util._
import scala.reflect.io.{AbstractFile, VirtualDirectory, VirtualFile}
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.{Level, Logger}

/**
  * Handles the interaction between scala-js-fiddle and
  * scalac/scalajs-tools to compile and optimize code submitted by users.
  */
class Compiler(classPath: Classpath, minLevel: JsLevel = JsLevel.Debug) { self =>
  
  val log = Logger.getLogger(getClass.getName)
  val sjsLogger = new SjLogger(minLevel)
  val extLibs = Nil

  /**
    * Converts a bunch of bytes into Scalac's weird VirtualFile class
    */
  def makeFile(src: Array[Byte]) = {
    val singleFile = new io.VirtualFile("ScalaFiddle.scala")
    val output = singleFile.output
    output.write(src)
    output.close()
    singleFile
  }

  def inMemClassloader = {
    new ClassLoader(this.getClass.getClassLoader) {
      val classCache = mutable.Map.empty[String, Option[Class[_]]]
      override def findClass(name: String): Class[_] = {
        log.fine("Looking for Class " + name)
        val fileName = name.replace('.', '/') + ".class"
        val res = classCache.getOrElseUpdate(
          name,
          classPath.compilerLibraries(extLibs)
            .map(_.lookupPathUnchecked(fileName, false))
            .find(_ != null)
            .map { f =>
              val data = f.toByteArray
              this.defineClass(name, data, 0, data.length)
            }
        )
        res match {
          case None =>
            log.warning("Not Found Class " + name)
            throw new ClassNotFoundException()
          case Some(cls) =>
            log.fine("Found Class " + name)
            cls
        }
      }
    }
  }

  /**
    * Mixed in to make a Scala compiler run entirely in-memory,
    * loading its classpath and running macros from pre-loaded
    * in-memory files
    */
  trait InMemoryGlobal { g: scala.tools.nsc.Global =>
    def ctx: JavaContext
    def dirs: Vector[DirectoryClassPath]
    override lazy val plugins = List[Plugin](new org.scalajs.core.compiler.ScalaJSPlugin(this))
    override lazy val platform: ThisPlatform = new JavaPlatform {
      val global: g.type = g
      override def classPath = new JavaClassPath(dirs, ctx)
    }
  }

  /**
    * Code to initialize random bits and pieces that are needed
    * for the Scala compiler to function, common between the
    * normal and presentation compiler
    */
  def initGlobalBits(logger: String => Unit) = {
    val vd = new io.VirtualDirectory("(memory)", None)
    val jCtx = new JavaContext()
    val jDirs = classPath.compilerLibraries(extLibs).map(new DirectoryClassPath(_, jCtx)).toVector
    lazy val settings = new Settings

    settings.outputDirs.setSingleOutput(vd)
    val writer = new Writer {
      def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
        val text = new String(cbuf)
        logger.apply(text.trim)
      }
      def flush(): Unit = {
      }
      def close(): Unit = ()
    }
    val reporter = new ConsoleReporter(settings, scala.Console.in, new PrintWriter(writer))
    (settings, reporter, vd, jCtx, jDirs)
  }

  def compile(src: List[VirtualFile], logger: String => Unit = _ => ()): Option[Seq[VirtualScalaJSIRFile]] = {
    val (settings, reporter, vd, jCtx, jDirs) = initGlobalBits(logger)
    val compiler = new nsc.Global(settings, reporter) with InMemoryGlobal {
      g =>
      def ctx = jCtx
      def dirs = jDirs
      override lazy val analyzer = new {
        val global: g.type = g
      } with Analyzer {
        val cl = inMemClassloader
        override def findMacroClassLoader() = cl
      }
    }

    log.info("Starting compilation ...")
    val run = new compiler.Run()
    run.compileFiles(src)

    if (vd.iterator.isEmpty)
      None
    else {
      def findSjsirFiles(vd : AbstractFile) : Iterator[AbstractFile] = {
        (vd.iterator.filter(_.isDirectory), vd.iterator.filter(_.name.endsWith(".sjsir"))) match {
          case (folders, sjFiles) =>
          sjFiles ++ folders.flatMap(findSjsirFiles)
        }
      }
      Some(findSjsirFiles(vd).map {
        case x =>
          val f = new MemVirtualSerializedScalaJSIRFile(x.path)
          f.content = x.toByteArray
          f: VirtualScalaJSIRFile
      }.toSeq)
    }
  }

  def export(output: VirtualJSFile): String = {
    log.fine(s"output content: ${output.content}")
    output.content
  }

  def fastOpt(userFiles: Seq[VirtualScalaJSIRFile]): VirtualJSFile =
    link(userFiles, fullOpt = false)

  def fullOpt(userFiles: Seq[VirtualScalaJSIRFile]): VirtualJSFile =
    link(userFiles, fullOpt = true)

  def link(userFiles: Seq[VirtualScalaJSIRFile], fullOpt: Boolean): VirtualJSFile = {
    val semantics =
      if (fullOpt) Semantics.Defaults.optimized
      else Semantics.Defaults

    val linker = Linker(
      semantics = semantics,
      withSourceMap = true,
      useClosureCompiler = fullOpt)

    val output = WritableMemVirtualJSFile("output.js")
    linker.link(classPath.linkerLibraries(extLibs) ++ userFiles, output, sjsLogger)
    output
  }

  class SjLogger(minLevel: JsLevel = JsLevel.Debug) extends JsLogger {
    def log(level: JsLevel, message: =>String): Unit = if (level >= minLevel) {
      if (level == JsLevel.Warn || level == JsLevel.Error)
        self.log.warning(message)
      else
        self.log.fine(message)
    }
    def success(message: => String): Unit = info(message)
    def trace(t: => Throwable): Unit = {
      self.log.log(Level.WARNING, "Compiling error", t)
    }
  }

}
