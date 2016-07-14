package fiddle

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import scala.reflect.io.Path.string2path
import scala.reflect.io.Streamable
import scala.reflect.io.VirtualDirectory

import org.scalajs.core.tools.io.IRFileCache
import org.scalajs.core.tools.io.MemVirtualBinaryFile
import org.scalajs.core.tools.io.VirtualJarFile
import org.slf4j.LoggerFactory

object Classpath {
  
  private lazy val build : (Class[_], String) => Classpath = (klass, relativeJarPath) => new Classpath(klass, relativeJarPath)
  
  def apply(klass : Class[_], relativeJarPath : String) = build(klass, relativeJarPath)
}

/**
 * Loads the jars that make up the classpath of the scala-js-fiddle
 * compiler and re-shapes it into the correct structure to satisfy
 * scala-compile and scalajs-tools
 */
class Classpath(context: Class[_], relativeJarPath : String) {

  val log = LoggerFactory.getLogger(getClass)
  val timeout = 60.seconds

  val baseLibs = Seq(
    s"scala-library-${Config.scalaVersion}.jar",
    s"scala-reflect-${Config.scalaVersion}.jar",
    s"scalajs-library_${Config.scalaMainVersion}-${Config.scalaJSVersion}.jar",
    s"scalajs-dom_sjs${Config.scalaJSMainVersion}_${Config.scalaMainVersion}-0.9.0.jar"
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

  val commonLibraries = {
    log.info("Loading files...")
    // load all external libs in parallel using spray-client
    val jarFiles = baseLibs.par.map { name =>
      val stream = context.getResourceAsStream(relativeJarPath + name)
      log.debug(s"Loading resource $name")
      if (stream == null) {
        throw new Exception(s"Classpath loading failed, jar $name not found at '${context.getResource(".")}' with realtive JAR path '$relativeJarPath'")
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
    log.info("Files loaded...")
    jarFiles ++ bootFiles
  }

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
          (x, Streamable.bytes(in))
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

  /**
   * In memory cache of all the jars used in the linker.
   */
  val commonLibraries4linker =
    commonLibraries.map { case (name, data) => lib4linker(name, data) }

  val linkerCaches = mutable.Map.empty[List[String], Seq[IRFileCache.VirtualRelativeIRFile]]

  def compilerLibraries(extLibs: List[String]) = {
    commonLibraries4compiler
  }

  def linkerLibraries(extLibs: List[String]) = {
    linkerCaches.getOrElseUpdate(extLibs, {
      val loadedJars = commonLibraries4linker
      val cache = (new IRFileCache).newCache
      val res = cache.cached(loadedJars)
      log.debug("Loaded scalaJSClassPath")
      res
    })
  }
}
