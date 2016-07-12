package fiddle

import java.io._
import java.nio.file.Files
import java.util.zip.ZipInputStream

//import akka.actor.ActorSystem
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.model._
//import akka.stream.ActorMaterializer
//import akka.stream.scaladsl.FileIO
import org.scalajs.core.tools.io._
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.reflect.io.{ Streamable, VirtualDirectory }
import javax.servlet.ServletContext

object Classpath {
  
  private lazy val build : ServletContext => Classpath = (context) => new Classpath(context)
  
  def apply(context : ServletContext) = build(context)
}

/**
 * Loads the jars that make up the classpath of the scala-js-fiddle
 * compiler and re-shapes it into the correct structure to satisfy
 * scala-compile and scalajs-tools
 */
class Classpath(context: ServletContext) {

  val log = LoggerFactory.getLogger(getClass)
  val timeout = 60.seconds

  val baseLibs = Seq(
    s"scala-library-${Config.scalaVersion}.jar",
    s"scala-reflect-${Config.scalaVersion}.jar",
    s"scalajs-library_${Config.scalaMainVersion}-${Config.scalaJSVersion}.jar",
    s"scalajs-dom_sjs${Config.scalaJSMainVersion}_${Config.scalaMainVersion}-0.9.0.jar",
    s"scalatags_${Config.scalaMainVersion}-0.5.4.jar"
    )

  val repoSJSRE = """([^ %]+) *%%% *([^ %]+) *% *([^ %]+)""".r
  val repoRE = """([^ %]+) *%% *([^ %]+) *% *([^ %]+)""".r
  val repoBase = "https://repo1.maven.org/maven2"
  val sjsVersion = s"_sjs${Config.scalaJSMainVersion}_${Config.scalaMainVersion}"

  def buildRepoUri(ref: String) = {
    ref match {
      case repoSJSRE(group, artifact, version) =>
        s"$repoBase/${group.replace('.', '/')}/$artifact$sjsVersion/$version/$artifact$sjsVersion-$version.jar"
      case repoRE(group, artifact, version) =>
        s"$repoBase/${group.replace('.', '/')}/${artifact}_${Config.scalaMainVersion}/$version/${artifact}_${Config.scalaMainVersion}-$version.jar"
      case _ => ref
    }
  }

  def loadExtLib(ref: String) = {
    val uri = buildRepoUri(ref)
    val name = uri.split('/').last
    // check if it has been loaded already
    val f = new File(Config.libCache, name)
    if (f.exists()) {
      log.info(s"Loading $name from ${Config.libCache}")
        (name -> Files.readAllBytes(f.toPath)) 
    } else {
      log.info(s"Loading $name from $uri")
      f.getParentFile.mkdirs()
      try {
        (name -> Array[Byte]())
      } catch {
        case e: Exception =>
          log.info(s"Error loading $uri: $e")
          throw e
      }
    }
  }

  val commonLibraries = {
    log.info("Loading files...")
    // load all external libs in parallel using spray-client
    val jarFiles = baseLibs.par.map { name =>
      val stream = context.getResourceAsStream("/WEB-INF/lib/" + name)
      log.debug(s"Loading resource $name")
      if (stream == null) {
        throw new Exception(s"Classpath loading failed, jar $name not found")
      }
      name -> Streamable.bytes(stream)
    }.seq

    val bootFiles = for {
      prop <- Seq( /*"java.class.path", */ "sun.boot.class.path")
      path <- System.getProperty(prop).split(System.getProperty("path.separator"))
      vfile = scala.reflect.io.File(path)
      if vfile.exists && !vfile.isDirectory
    } yield {
      path.split("/").last -> vfile.toByteArray()
    }
    log.debug("Files loaded...")
    jarFiles ++ bootFiles
  }

  /**
   * External libraries loaded from repository
   */
//  val extLibraries = {
//    Config.extLibs.map {
//      case (name, ref) =>
//        (name -> loadExtLib(ref))
//    }
//  }

  /**
   * The loaded files shaped for Scalac to use
   */
  def lib4compiler(name: String, bytes: Array[Byte]) = {
    log.info(s"Loading $name for Scalac")
    val in = new ZipInputStream(new ByteArrayInputStream(bytes))
    val entries = Iterator
      .continually({
        try {
          in.getNextEntry
        } catch {
          case e : Exception =>
            null
        }
      })
      .takeWhile(_ != null)
      .map(x => {
        try {
          (x, Streamable.bytes(in))
        } catch {
          case e : Exception =>
            (x, Array[Byte]())
        }
      })

    val dir = new VirtualDirectory(name, None)
    for {
      (e, data) <- entries
      if !e.isDirectory
    } {
      val tokens = e.getName.split("/")
      var d = dir
      for (t <- tokens.dropRight(1)) {
        d = d.subdirectoryNamed(t).asInstanceOf[VirtualDirectory]
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
  def lib4linker(name: String, bytes: Array[Byte]) = {
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
    commonLibraries.map { case (name, data) => 
          lib4compiler(name, data)
          }
  }
//  val extLibraries4compiler =
//    extLibraries.map { case (key, (name, data)) => key -> lib4compiler(name, data) }

  /**
   * In memory cache of all the jars used in the linker.
   */
  val commonLibraries4linker =
    commonLibraries.map { case (name, data) => lib4linker(name, data) }
//  val extLibraries4linker =
//    extLibraries.map { case (key, (name, data)) => key -> lib4linker(name, data) }

  val linkerCaches = mutable.Map.empty[List[String], Seq[IRFileCache.VirtualRelativeIRFile]]

  def compilerLibraries(extLibs: List[String]) = {
    commonLibraries4compiler // ++ extLibs.flatMap(extLibraries4compiler.get)
  }

  def linkerLibraries(extLibs: List[String]) = {
    linkerCaches.getOrElseUpdate(extLibs, {
      val loadedJars = commonLibraries4linker // ++ extLibs.flatMap(extLibraries4linker.get)
      val cache = (new IRFileCache).newCache
      val res = cache.cached(loadedJars)
      log.debug("Loaded scalaJSClassPath")
      res
    })
  }
}
