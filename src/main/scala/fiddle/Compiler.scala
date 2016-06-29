package fiddle

import java.io.{PrintWriter, Writer}

import akka.util.ByteString
import org.scalajs.core.tools.io._
import org.scalajs.core.tools.linker.Linker
import org.scalajs.core.tools.logging._
import org.scalajs.core.tools.sem.Semantics
import org.slf4j.LoggerFactory

import scala.async.Async.async
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

/**
  * Handles the interaction between scala-js-fiddle and
  * scalac/scalajs-tools to compile and optimize code submitted by users.
  */
class Compiler(classPath: Classpath, env: String) { self =>
  val log = LoggerFactory.getLogger(getClass)
  val sjsLogger = new Log4jLogger()
  val blacklist = Set("<init>")
  val extLibs = Config.environments.getOrElse(env, Nil)

  /**
    * Converts Scalac's weird Future type
    * into a standard scala.concurrent.Future
    */
  def toFuture[T](func: Response[T] => Unit): Future[T] = {
    val r = new Response[T]
    Future {func(r); r.get.left.get}
  }

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
        log.debug("Looking for Class " + name)
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
            log.debug("Not Found Class " + name)
            throw new ClassNotFoundException()
          case Some(cls) =>
            log.debug("Found Class " + name)
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
    val jDirs = classPath.compilerLibraries(extLibs).map(new DirectoryClassPath(_, jCtx)).toVector
    lazy val settings = new Settings

    settings.outputDirs.setSingleOutput(vd)
    val writer = new Writer {
      var inner = ByteString()
      def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
        inner = inner ++ ByteString.fromArray(cbuf.map(_.toByte), off, len)
      }
      def flush(): Unit = {
        logger(inner.utf8String)
        inner = ByteString()
      }
      def close(): Unit = ()
    }
    val reporter = new ConsoleReporter(settings, scala.Console.in, new PrintWriter(writer))
    (settings, reporter, vd, jCtx, jDirs)
  }

  def getTemplate(template: String) = {
    Config.templates.get(template) match {
      case Some(t) => t
      case None => throw new IllegalArgumentException(s"Invalid template $template")
    }
  }

  def autocomplete(templateId: String, code: String, flag: String, pos: Int): Future[List[(String, String)]] = async {
    import scala.tools.nsc.interactive._

    val template = getTemplate(templateId)
    // global can be reused, just create new runs for new compiler invocations
    val (settings, reporter, vd, jCtx, jDirs) = initGlobalBits(_ => ())
    settings.processArgumentString("-Ypresentation-any-thread")
    val compiler = new nsc.interactive.Global(settings, reporter) with InMemoryGlobal {
      g =>
      def ctx = jCtx
      def dirs = jDirs
      override lazy val analyzer = new {
        val global: g.type = g
      } with InteractiveAnalyzer {
        val cl = inMemClassloader
        override def findMacroClassLoader() = cl
      }
    }

    val offset = pos + template.pre.length
    val fullSource = template.fullSource(code)
    val source = fullSource.take(offset) + "_CURSOR_ " + fullSource.drop(offset)
    val run = new compiler.TyperRun
    val unit = compiler.newCompilationUnit(source, "ScalaFiddle.scala")
    val richUnit = new compiler.RichCompilationUnit(unit.source)
    log.debug(s"Source: ${source.take(offset)}${scala.Console.RED}|${scala.Console.RESET}${source.drop(offset)}")
    compiler.unitOfFile(richUnit.source.file) = richUnit
    val results = compiler.completionsAt(richUnit.position(offset)).matchingResults()

    log.debug(s"Completion results: ${results.take(20)}")

    results.map(r => (r.sym.signatureString, r.symNameDropLocal.decoded)).distinct
  }

  def compile(templateId: String, src: String, logger: String => Unit = _ => ()): Option[Seq[VirtualScalaJSIRFile]] = {

    val template = getTemplate(templateId)
    val fullSource = template.fullSource(src)
    log.debug("Compiling source:\n" + fullSource)
    val singleFile = makeFile(fullSource.getBytes("UTF-8"))

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
    run.compileFiles(List(singleFile))

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
      if (level == Level.Warn || level == Level.Error)
        self.log.error(message)
      else
        self.log.debug(message)
    }
    def success(message: => String): Unit = info(message)
    def trace(t: => Throwable): Unit = {
      self.log.error("Compiling error", t)
    }
  }

}
