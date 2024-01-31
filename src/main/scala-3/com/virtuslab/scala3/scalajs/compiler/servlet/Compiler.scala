package com.virtuslab.scala3.scalajs.compiler


import org.scalajs.linker.{StandardImpl, MemOutputDirectory}
import org.scalajs.linker.standard.MemIRFileImpl
import org.scalajs.logging.{Logger => JsLogger, Level => JsLevel}
import org.scalajs.linker.interface.StandardConfig

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import dotty.tools.Settings
import dotty.tools.dotc.config.JavaPlatform
import dotty.tools.io.VirtualDirectory
import dotty.tools.io.AbstractFile
import dotty.tools.io.VirtualFile
import java.nio.file.Files
import java.nio.file.Paths
import java.util.logging.{ Logger, Level }

import dotty.tools.dotc.core.Contexts.ContextBase
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.Driver
import dotty.tools.dotc.config.Platform
import dotty.tools.dotc.classpath.ClassPathFactory
import dotty.tools.dotc.config.SJSPlatform
import dotty.tools.dotc.classpath.AggregateClassPath
import dotty.tools.dotc.config.PathResolver
import dotty.tools.io.ClassPath
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import org.scalajs.linker.interface.OutputDirectory
import org.scalajs.linker.PathOutputDirectory
import java.nio.file.Path
import org.scalajs.linker.interface.ModuleInitializer
import dotty.tools.dotc.ast.untpd.Mod.Abstract
import dotty.tools.dotc.classpath.JrtClassPath

/**
  * Handles the interaction between this and
  * scalac/scalajs-linker to compile and optimize code submitted by users.
  */
class Compiler(compilerClassPath: Classpath, env: String) { self =>

  private val log = Logger.getLogger(getClass.getName)
  private val sjsLogger = new SjLogger()
  private val extLibs = Config.environments.getOrElse(env, Nil)

  /**
    * Converts a bunch of bytes into Scalac's weird VirtualFile class
    */
  def makeFile(src: Array[Byte]) = {
    log.info("Creating virtual file ...")
    val singleFile = new VirtualFile("ScalaFiddle.scala")
    val output = singleFile.output
    output.write(src)
    output.close()
    singleFile
  }

  def compile(src: CompileActor.Source, logger: String => Unit = _ => ()): Option[(Seq[MemIRFileImpl], Seq[String])] = {
    log.info("Preparing compilation ...")
    val vd = new VirtualDirectory("(memory)", None)

    // loading bootfiles, copy/pasted from the dotty (scala 3) github repository 
    val rtClassPath: ClassPath = {
      given Context = new ContextBase().initialCtx
      val specVersion = scala.util.Properties.javaSpecVersion
      if (specVersion == "" || specVersion == "1.8") {
        val resolver = new PathResolver
        val elements = (new ClassPathFactory).classesInPath(resolver.Calculated.javaBootClassPath)
        AggregateClassPath(elements)
      }
      else JrtClassPath(None).get
    }

    class InMemoryContextBase() extends ContextBase() {
      override def newPlatform(using Context): Platform = new SJSPlatform {
        private val currentClassPath = 
          AggregateClassPath.createAggregate(compilerClassPath.compilerLibraries(extLibs).map(ClassPathFactory.newClassPath(_)) :+ rtClassPath: _*)
        override def classPath(using Context) = currentClassPath
        override def updateClassPath(subst: Map[ClassPath, ClassPath]): Unit = ???
      }
    }

    val context = new InMemoryContextBase().initialCtx
    val settings = context.settings
    val settingsWithOutput = settings.outputDir.updateIn(context.settingsState, vd)
    val settingsWithExplain = settings.explain.updateIn(settingsWithOutput, true)
    val settingsWithOutputWithJs = settings.scalajs.updateIn(settingsWithExplain, true)
    val contextWithSettings = context.fresh.setSettings(settingsWithOutputWithJs)

    val compiler = new dotty.tools.dotc.Compiler()
    new PathResolver(using contextWithSettings).result
    log.info("Running compilation ...")
    val compilerRun = compiler.newRun(using contextWithSettings)
    compilerRun.compile(src)

    if (vd.iterator.isEmpty) {
      log.info("No result.")
      None
    } else {
      def findFilesWithExtension(vd: AbstractFile, ext: String): Iterator[AbstractFile] =
        (vd.iterator.filter(_.isDirectory), vd.iterator.filter(_.name.endsWith(ext))) match {
          case (folders, filesWithExtension) =>
            filesWithExtension ++ folders.flatMap(findFilesWithExtension(_, ext))
        }
      def findSjsirFiles(vd: AbstractFile): Iterator[AbstractFile] =
        findFilesWithExtension(vd, ".sjsir")
      def findClassFiles(vd: AbstractFile): Iterator[AbstractFile] =
        findFilesWithExtension(vd, ".class")
  
      val sjsirFiles =
        findSjsirFiles(vd).map {
          case x => {
            log.info(s"Found SJSIR file ${x.name}")
            new MemIRFileImpl(x.path, None, x.toByteArray)
          }
        }.toSeq
      log.info(s"Finding main class in virtual directory: ${vd.name}, files: ${vd.toList}")
      val mainClasses = MainClass.find(findClassFiles(vd).toList)
      log.info("SJS files:" + sjsirFiles + ", main classes:" + mainClasses)
      Some(sjsirFiles, mainClasses)
    }
  }

  def `export`(output: MemOutputDirectory): String = {
    log.info(s"output files: ${output.fileNames()}")
    val mainJs = output.content("main.js")
    if (mainJs.isEmpty)
      log.warning("No result!")
    mainJs.map(new String(_)).getOrElse("// No JavaScript!")
  }

  def fastOpt(userFiles: Seq[MemIRFileImpl], mainClasses: Seq[String]): MemOutputDirectory =
    link(userFiles, mainClasses, fullOpt = false)

  def fullOpt(userFiles: Seq[MemIRFileImpl], mainClasses: Seq[String]): MemOutputDirectory =
    link(userFiles, mainClasses, fullOpt = true)

  private def link(userFiles: Seq[MemIRFileImpl], mainClasses: Seq[String], fullOpt: Boolean): MemOutputDirectory = {
    val moduleInitializers = mainClasses.map(ModuleInitializer.mainMethodWithArgs(_, "main"))
    log.info("module inits:" + moduleInitializers)
    
    val config = StandardConfig()
      .withClosureCompiler(fullOpt)

    val linkerConfig = if (fullOpt) {
      config.withSemantics(_.optimized)
    } else {
      config
    }

    val linker = StandardImpl.linker(linkerConfig)

    val output = MemOutputDirectory()
    val reportFuture = linker.link(userFiles ++ compilerClassPath.linkerLibraries(extLibs), moduleInitializers, output, sjsLogger)
    val res = Await.result(reportFuture, Duration.Inf)

    output
  }

  private class SjLogger(minLevel: JsLevel = JsLevel.Debug) extends JsLogger {
    def log(level: JsLevel, message: => String): Unit = 
      if (level >= minLevel) {
        if (level == JsLevel.Warn || level == JsLevel.Error) {
          self.log.warning(s"Warning or Error: $message")
        } else {
          self.log.fine(s"Message: $message")
        }
      }
    def trace(t: => Throwable): Unit = {
      self.log.log(Level.WARNING, "Exception thrown!", t)
    }
  }

}
class MyVirtualDirectory(name: String, maybeContainer: Option[VirtualDirectory] = None) extends VirtualDirectory(name, maybeContainer) {
  val files = mutable.Map.empty[String, AbstractFile]

  val log = Logger.getLogger(getClass.getName)

  override def iterator(): Iterator[AbstractFile] = files.values.toList.iterator

  override def fileNamed(name: String): AbstractFile =
    Option(lookupName(name, directory = false)) getOrElse {
//      log.info(s"New file at path/name: $path/$name") // LOTS of logging
      val newFile = new MyVirtualFile(name, s"$path/$name", this)
      files(name) = newFile
      newFile
    }

  override def subdirectoryNamed(name: String): AbstractFile =
    Option(lookupName(name, directory = true)) getOrElse {
      val dir = new MyVirtualDirectory(name, Some(this))
      files(name) = dir
      dir
    }

  override def clear(): Unit = {
    files.clear()
  }
  override def lookupName(name: String, directory: Boolean): AbstractFile = {
    (files get name filter (_.isDirectory == directory)).orNull
  }
}

class MyVirtualFile(name: String, path: String, _container: AbstractFile) extends VirtualFile(name, path) {
  override def container: AbstractFile = _container
}