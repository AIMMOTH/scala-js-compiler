package io.cenet.compiler

import java.io.{PrintWriter, Writer}
import org.scalajs.core.tools.io._
import org.scalajs.core.tools.linker.Linker
import org.scalajs.core.tools.logging._
import org.scalajs.core.tools.sem.Semantics
//import org.slf4j.LoggerFactory
//import scala.async.Async.async
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
import java.io.File
import scala.reflect.io.VirtualDirectory
import java.util.logging.{Logger => JLogger}

object VirtualSjsCompiler {

  def apply() = {
    new VirtualSjsCompiler(new VirtualClasspath, "") match {
      case compiler => compiler()
    }
  }
}

class VirtualSjsCompiler(classPath: VirtualClasspath, env: String) { self =>
  val log = JLogger.getLogger(classOf[VirtualSjsCompiler].getName)
  val sjsLogger = new Log4jLogger()
  val blacklist = Set("<init>")
  val extLibs = VirtualConfig.environments.getOrElse(env, Nil)

  /**
    * Converts Scalac's weird Future type
    * into a standard scala.concurrent.Future
    */
//  def toFuture[T](func: Response[T] => Unit): Future[T] = {
//    val r = new Response[T]
//    Future {func(r); r.get.left.get}
//  }

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
            .find(_ != null).map { f =>
            val data = f.toByteArray
            this.defineClass(name, data, 0, data.length)
          }
        )
        res match {
          case None =>
            log.fine("Not Found Class " + name)
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
  trait InMemoryGlobal {
    g: scala.tools.nsc.Global =>
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
    val virtuals = classPath.compilerLibraries(extLibs)
    val jDirs = virtuals.map(new DirectoryClassPath(_, jCtx)).toVector
    lazy val settings = new Settings
    settings.deprecation.value = true
    settings.unchecked.value = true
    settings.outputDirs.setSingleOutput(vd)
    val writer = new Writer {
      val sb = new StringBuilder();
//      var inner = ByteString()
      def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
//        inner = inner ++ ByteString.fromArray(cbuf.map(_.toByte), off, len)
        val line = cbuf.map(_.toByte)
        log.fine(new String(line))
        sb.append(line)
      }
      def flush(): Unit = {
        logger(sb.toString())
//        logger(inner.utf8String)
//        inner = ByteString()
        sb.clear()
      }
      def close(): Unit = ()
    }
    val reporter = new ConsoleReporter(settings, scala.Console.in, new PrintWriter(writer))
    (settings, reporter, vd, jCtx, jDirs)
  }

//  def getTemplate(template: String) = {
//    VirtualConfig.templates.get(template) match {
//      case Some(t) => t
//      case None => throw new IllegalArgumentException(s"Invalid template $template")
//    }
//  }

  implicit class Pipeable[T](t: T){
    def |>[V](f: T => V): V = f(t)
  }

  def compile(logger: String => Unit = _ => ()): Option[Seq[VirtualScalaJSIRFile]] = {

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

    val run = new compiler.Run()
    
    val s = new scala.reflect.io.VirtualFile("test.scala")
    val b = s.output
    b.write("""
      package test
      import scala.scalajs.js.JSApp
      import scala.scalajs.js.annotation.JSExport
      @JSExport
      object Test extends JSApp {
        def main() : Unit = {
        println("Hello!!")
        }
        @JSExport
        def click() = {
          println("Hello!")
        }
      }
      """.getBytes("UTF-8"))
//      b.flush()
      b.close()
      
//    run.compileFiles(files.toList)
      run.compileFiles(List(s))
      println("Errors?" + compiler.reporter.hasErrors)

    if (vd.iterator.isEmpty) None
    else {
      val things = for {
        x <- vd.iterator.to[collection.immutable.Traversable]
        if x.name.endsWith(".sjsir")
      } yield {
        val f = new MemVirtualSerializedScalaJSIRFile(x.path)
        f.content = x.toByteArray
        f: VirtualScalaJSIRFile
      }
      Some(things.toSeq)
    }
  }

  def apply() = compile().map(_ |> fullOpt _ |> export)

  def export(output: VirtualJSFile): String =
    output.content

  def fastOpt(userFiles: Seq[VirtualScalaJSIRFile]): VirtualJSFile =
    link(userFiles, fullOpt = false)

  def fullOpt(userFiles: Seq[VirtualScalaJSIRFile]): VirtualJSFile =
    link(userFiles, fullOpt = true)

  def link(userFiles: Seq[VirtualScalaJSIRFile],
    fullOpt: Boolean): VirtualJSFile = {
    val semantics =
      if (fullOpt) Semantics.Defaults.optimized
      else Semantics.Defaults

    val linker = Linker(
      semantics = semantics,
      withSourceMap = false,
      useClosureCompiler = fullOpt)

    val output = WritableMemVirtualJSFile("output.js")
    linker.link(classPath.linkerLibraries(extLibs) ++ userFiles, output, sjsLogger)
    output
  }

  class Log4jLogger(minLevel: Level = Level.Debug) extends Logger {

    def log(level: Level, message: =>String): Unit = if (level >= minLevel) {
//      if (level == Level.Warn || level == Level.Error)
//        self.log.error(message)
//      else
//        self.log.debug(message)
    }
    def success(message: => String): Unit = info(message)
    def trace(t: => Throwable): Unit = {
//      self.log.error("Compiling error", t)
    }
  }

}
