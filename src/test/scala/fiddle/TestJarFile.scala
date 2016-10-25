package fiddle

import org.junit.Test
import java.util.zip.ZipInputStream
import java.io.FileInputStream
import java.util.zip.ZipFile
import java.io.File

class TestJarFile {

  @Test
  def readJarFileWithScalaJsSource : Unit = {
    new ScalaJsCompiler match {
      case compiler =>
        getClass.getClassLoader.getResource("ScalaJsSource.jar") match {
          case path =>
            new ZipFile(path.getFile) match {
              case jarFile =>
                compiler.compileJarWithScalaJsSource(getClass.getClassLoader, jarFile, Optimizer.Fast, "") match {
                  case compiled =>
                    println(s"Compiled size ${compiled.length}B")
                }
            }
        }
    }
  }
}