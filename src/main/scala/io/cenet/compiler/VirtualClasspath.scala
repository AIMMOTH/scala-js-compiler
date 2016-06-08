package io.cenet.compiler

import scala.collection.JavaConverters._
import java.io._
import java.util.zip.ZipInputStream
import org.scalajs.core.tools.io._
//import org.slf4j.LoggerFactory
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.io.{Streamable, VirtualDirectory}
import java.util.zip.ZipInputStream
import org.scalajs.core.tools.io._
//import org.slf4j.LoggerFactory
import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.io.{Streamable, VirtualDirectory}
import com.google.common.io.ByteStreams
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.logging.Logger

/**
  * Loads the jars that make up the classpath of the scala-js-fiddle
  * compiler and re-shapes it into the correct structure to satisfy
  * scala-compile and scalajs-tools
  */
class VirtualClasspath {
//  implicit val system = ActorSystem()
//  implicit val materializer = ActorMaterializer()
//  implicit val ec = system.dispatcher

  val log = Logger.getLogger(classOf[VirtualClasspath].getName)
//  val timeout = 60.seconds

//  val baseLibs = Seq(
//    s"/scala-library-${VirtualConfig.scalaVersion}.jar",
//    s"/scala-reflect-${VirtualConfig.scalaVersion}.jar",
//    s"/scalajs-library_${VirtualConfig.scalaMainVersion}-${VirtualConfig.scalaJSVersion}.jar"
//    ,
//    s"/page_sjs${VirtualConfig.scalaJSMainVersion}_${VirtualConfig.scalaMainVersion}-${VirtualConfig.version}.jar"
//  )

  val repoSJSRE = """([^ %]+) *%%% *([^ %]+) *% *([^ %]+)""".r
  val repoRE = """([^ %]+) *%% *([^ %]+) *% *([^ %]+)""".r
//  val repoBase = "https://repo1.maven.org/maven2"
  val sjsVersion = s"_sjs${VirtualConfig.scalaJSMainVersion}_${VirtualConfig.scalaMainVersion}"
  val init = new WebappInit

//  def buildRepoUri(ref: String) = {
//    ref match {
//      case repoSJSRE(group, artifact, version) =>
//        s"$repoBase/${group.replace('.', '/')}/$artifact$sjsVersion/$version/$artifact$sjsVersion-$version.jar"
//      case repoRE(group, artifact, version) =>
//        s"$repoBase/${group.replace('.', '/')}/${artifact}_${VirtualConfig.scalaMainVersion}/$version/${artifact}_${VirtualConfig.scalaMainVersion}-$version.jar"
//      case _ => ref
//    }
//  }

//  def loadExtLib(ref: String) = {
//    val uri = buildRepoUri(ref)
//    val name = uri.split('/').last
//    // check if it has been loaded already
//    val f = new File(VirtualConfig.libCache, name)
//    log.debug(s"Loading $name from ${VirtualConfig.libCache}")
//    (name, Files.readAllBytes(f.toPath))
//  }

  val commonLibraries = {
//    log.debug(s"Loading files ${baseLibs}")
    // load all external libs in parallel using spray-client

//    val jarFiles = baseLibs.par.map { name =>
//      log.debug(s"Loading $name ...")
//      val stream = getClass.getResourceAsStream(name)
//      log.debug(s"Loading resource $name")
//      if (stream == null) {
//        throw new Exception(s"Classpath loading failed, jar $name not found")
//      }
////      name -> Streamable.bytes(stream)
//      name -> ByteStreams.toByteArray(stream)
//    }.seq

    val bootFiles = for {
      prop <- Seq(/*"java.class.path", */ "sun.boot.class.path")
      path <- System.getProperty(prop).split(System.getProperty("path.separator"))
      vfile = scala.reflect.io.File(path)
      if vfile.exists && !vfile.isDirectory
    } yield {
      log.fine(s"Loading ${vfile}")
      path.split("/").last -> Array[Byte]() //vfile.toByteArray()
    }
//    log.debug("Files loaded...")
//    log.debug(s"Loaded ${JarFiles.jarFiles.map(_._1).mkString}")
    JarFiles.jarFiles ++ bootFiles
//    JarFiles.files.map{
//      case file =>
//        log.info(s"Loading ${file}")
//        val buffer = new ByteArrayOutputStream();
//        val stream = getClass.getResourceAsStream(file)
//
//        log.info(s"Stream ${stream}")
//
//        if (stream != null) {
//          var nRead = 0
//          val data = Array[Byte]()
//
//          while ((nRead = stream.read(data, 0, data.length)) != -1) {
//            buffer.write(data, 0, nRead);
//          }
//
//          buffer.flush();
//
//          file -> buffer.toByteArray()
//        } else {
//          file -> Array[Byte]()
//        }
//    }.seq
  }

  /**
    * External libraries loaded from repository
    */
//  val extLibraries = {
//    VirtualConfig.extLibs.map { case (name, ref) =>
//      (name -> loadExtLib(ref))
//    }
//  }

  /**
    * The loaded files shaped for Scalac to use
    */
  def lib4compiler(name: String, bytes: Array[Byte]) = {
//    val in = new ZipInputStream(new ByteArrayInputStream(bytes))
//    val entries = Iterator
//      .continually(in.getNextEntry)
//      .takeWhile(_ != null)
//      .map{
//        case e =>
//          (e, Streamable.bytes(in))
//      }

//    val dir = new VirtualDirectory(name, None)
//    for {
//      (e, data) <- entries
//      if !e.isDirectory
//    } {
//      val tokens = e.getName.split("/")
//      var d = dir
//      for (t <- tokens.dropRight(1)) {
//        d = d.subdirectoryNamed(t).asInstanceOf[VirtualDirectory]
//      }
//      val f = d.fileNamed(tokens.last)
//      val o = f.bufferedOutput
//      o.write(data)
//      o.close()
//    }
    val tokens = name.split("/")
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
      log.fine(s"${f.name} in ${f.canonicalPath}")
    val o = f.bufferedOutput
    o.write(bytes)
    o.close()

    dir
  }

  /**
    * The loaded files shaped for Scala-Js-Tools to use
    */
  def lib4linker(name: String, bytes : Array[Byte]) = {
    val jarFile = (new MemVirtualBinaryFile(name) with VirtualJarFile)
      .withContent(bytes)
      .withVersion(Some(name)) // unique through the lifetime of the server
    IRFileCache.IRContainer.Jar(jarFile)
  }

  /**
    * In memory cache of all the jars used in the compiler. This takes up some
    * memory but is better than reaching all over the filesystem every time we
    * want to do something.
    */
  val commonLibraries4compiler = {
    log.fine("Load files into Virtual directory for compilation ...")
//    commonLibraries.map { case (name, data) => lib4compiler(name, data) }
    JarFiles.jarFiles
  }

//  val extLibraries4compiler =
//    extLibraries.map { case (key, (name, data)) => key -> lib4compiler(name, data) }

  /**
    * In memory cache of all the jars used in the linker.
    */
  val commonLibraries4linker =
    JarFiles.jarBytes.map { case (name, data) => lib4linker(name, data) }

//  val extLibraries4linker =
//    extLibraries.map { case (key, (name, data)) => key -> lib4linker(name, data) }

  val linkerCaches = mutable.Map.empty[List[String], Seq[IRFileCache.VirtualRelativeIRFile]]

  def compilerLibraries(extLibs: List[String]) = {
    commonLibraries4compiler
//    ++ extLibs.flatMap(extLibraries4compiler.get)
  }

  def linkerLibraries(extLibs: List[String]) = {
    linkerCaches.getOrElseUpdate(extLibs, {
      val loadedJars = commonLibraries4linker
//      ++ extLibs.flatMap(extLibraries4linker.get)
      val cache = (new IRFileCache).newCache
      val res = cache.cached(loadedJars)
      log.fine("Loaded scalaJSClassPath")
      res
    })
  }
}
