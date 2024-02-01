package com.virtuslab.scala3.scalajs.compiler


import dotty.tools.io.Streamable
import org.scalajs.linker.StandardImpl
import org.scalajs.linker.interface.IRFile
import org.scalajs.linker.interface.unstable.IRContainerImpl
import org.scalajs.linker.standard.MemIRFileImpl

import java.io.{ByteArrayInputStream, InputStream}
import java.util.logging.Logger
import java.util.zip.ZipInputStream
import scala.collection.mutable
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

object Classpath {

  private lazy val build: ((String => InputStream), String, Set[String]) => Classpath = (loader, relativeJarPath, libs) => new Classpath(loader, relativeJarPath, libs)

  def apply(loader: (String => InputStream), relativeJarPath: String, libs: Set[String] = Set()) = build(loader, relativeJarPath, libs)
}

/**
 * Loads the jars that make up the classpath of the scala-js-fiddle
 * compiler and re-shapes it into the correct structure to satisfy
 * scala-compile and scalajs-tools
 */
class Classpath(loader: (String => InputStream), relativeJarPath: String, libs: Set[String] = Set()) {

  private val log = Logger.getLogger(getClass.getName)

  private val commonLibraries = {
    // load all external libs in parallel using spray-client
    val jarFiles = libs.toSeq.map { name => // do in parallel
      val stream = loader.apply(relativeJarPath + name)
      log.info(s"Loading resource $name")
      if (stream == null) {
        throw new Exception(s"Classpath loading failed, jar $name not in relative path '$relativeJarPath'")
      }
      name -> Streamable.bytes(stream)
    }

    log.info("Files loaded...")

    jarFiles
  }
  
  private val virtualSet = commonLibraries.map {
    case (name, data) =>
      lib4compiler(name, data)
  }

  /**
   * The loaded files shaped for Scalac to use
   */
  private def lib4compiler(name: String, bytes: Array[Byte]) = {
    log.info(s"Loading $name for Scalac")
    val entries = readZipFile(bytes)

    val dir = new MyVirtualDirectory(name, None)
    for {
      (e, data) <- entries
      if !e.isDirectory
    } {
      val tokens = e.getName.split("/")
      var d = dir
      for (t <- tokens.dropRight(1)) {
        d = d.subdirectoryNamed(t).asInstanceOf[MyVirtualDirectory]
      }
      val f = d.fileNamed(tokens.last)
      val o = f.bufferedOutput
      o.write(data)
      o.close()
    }
    dir
  }

  /**
   * The loaded files shaped for Scala-Js-Tools to use
   */
  private def lib4linker(name: String, bytes: Array[Byte]) = {
    val entries = readZipFile(bytes)

    val files = mutable.ArrayBuffer[MemIRFileImpl]()
    for {
      (e, data) <- entries
      if !e.isDirectory
    } {
      val path = e.getName
      if (path.endsWith(".sjsir")) files.addOne(new MemIRFileImpl(path, None, data))
    }
    MemJarIRContainer(name, files.toList)
  }

  private def readZipFile(bytes: Array[Byte]) = {
    val in = new ZipInputStream(new ByteArrayInputStream(bytes))
    val entries = Iterator
      .continually({
        try {
          in.getNextEntry
        } catch {
          case e: Exception =>
            null
        }
      })
      .takeWhile(_ != null)
      .map(x => {
        (x, Streamable.bytes(in))
      }
      )
    entries
  }

  class MemJarIRContainer(path: String, files: List[MemIRFileImpl])
      extends IRContainerImpl(path, None) {
    def sjsirFiles(implicit ec: ExecutionContext): Future[List[IRFile]] = Future.successful(files)  
  }

  /**
   * In memory cache of all the jars used in the compiler. This takes up some
   * memory but is better than reaching all over the filesystem every time we
   * want to do something.
   */
  private val commonLibraries4compiler = virtualSet

  /**
   * In memory cache of all the jars used in the linker.
   */
  private val commonLibraries4linker = commonLibraries.map { case (name, data) => lib4linker(name, data) }

  private val linkerCaches = mutable.Map.empty[List[String], List[IRFile]]

  def compilerLibraries(extLibs: List[String]) = commonLibraries4compiler

  def linkerLibraries(extLibs: List[String]) = {
    linkerCaches.getOrElseUpdate(extLibs, {
      val loadedJars = commonLibraries4linker
      val cache = StandardImpl.irFileCache().newCache
      val res = Await.result(cache.cached(loadedJars)(ExecutionContext.global), Duration.Inf)
      log.fine("Loaded scalaJSClassPath")
      res.toList
    })
  }
}
