package com.github.aimmoth.scalajs.compiler

import com.github.scalafiddle.Optimizer
import com.github.scalafiddle.Util.Pipeable
import org.junit.jupiter.api.{Assertions, DisplayName, Test}
import org.scalajs.core.tools.logging.Level

import java.io.{File, FileInputStream, FileNotFoundException, InputStream}
import java.util.zip.ZipFile

class TestScalaJsCompiler {

  private val fastCompilationNotMinimized = Optimizer.Fast
  private val charsetName = "UTF-8"
  private val compilerLoggingLevel = Level.Info
  private val scalaJsCode =
    """
      |package tutorial.webapp
      |
      |object TutorialApp {
      |  def main(args: Array[String]): Unit = {
      |    println("Hello world!")
      |  }
      |}
      |""".stripMargin
  private val scalaJsFile = ScalaJsFile("Test", scalaJsCode)

  @DisplayName("Initiate ScalaJsCompiler and compile simple code")
  @Test
  def test_happyPath = {
    // Given
    val compiler = createCompilerAndInit

    // When
    val actual = compiler.compileScalaJsStrings(List(scalaJsFile), fastCompilationNotMinimized, charsetName, compilerLoggingLevel)

    // Then
    Assertions.assertTrue(actual.isRight)
    println("JavaScript length:" + actual.right.get.length)
  }

  @DisplayName("Should gracefully handle source code with bug")
  @Test
  def test_withBug = {
    // Given
    val bug = scalaJsCode + "bug"
    val compiler = createCompilerAndInit

    // When
    val actual = compiler.compileScalaJsStrings(List(ScalaJsFile("Test", bug)), fastCompilationNotMinimized, charsetName, compilerLoggingLevel)

    // Then
    Assertions.assertTrue(actual.isLeft)
  }

  @DisplayName("Should read jar file and compile content")
  @Test
  def test_readJarFileWithScalaJsSource : Unit = {
    // Given
    val compiler = createCompilerAndInit
    val zipFile = getClass.getClassLoader.getResource("ScalaJsSource.jar") |> (f => f.getFile) |> (file => new ZipFile(file))

    // When
    val actual = compiler.compileJarWithScalaJsSource(zipFile, fastCompilationNotMinimized)

    // Then
    Assertions.assertNotNull(actual)
    println("Javascript length:" + actual.length)
  }

  private def createCompilerAndInit = {
    val allJarsOnClassPathSeparated = System.getProperty("java.class.path")
    val separator = System.getProperty("path.separator");
    val jarsWithFullFilePath = allJarsOnClassPathSeparated.split(separator)

    val loader : (String => InputStream)= (jarFile: String) => {
      jarsWithFullFilePath.find(s => s.endsWith(jarFile)) match {
        case Some(found) => {
          println("Found on classpath:" + found)
          new FileInputStream(new File(found))
        }
        case None => throw new FileNotFoundException(jarFile)
      }
    }

    val relativeJarPath = "" // We get full path from above and do not need a relative path
    val additionalLibs = Set[String]() // We don't have any extra dependencies in this repository
    val baseLibs = Seq("scala-library-2.11.12.jar", "scala-reflect-2.11.12.jar", "scalajs-library_2.11-0.6.33.jar") // Always needed for compilation
    ScalaJsCompiler().init(loader, relativeJarPath, additionalLibs, baseLibs)
  }
}
