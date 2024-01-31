package com.virtuslab.scala3.scalajs.compiler.servlet

import com.virtuslab.scala3.scalajs.compiler.{Classpath, Optimizer, ScalaJsCompiler, ScalaJsFile}
import org.scalatest.funsuite.AnyFunSuite

import java.io.{File, FileInputStream, FileNotFoundException, InputStream}
import java.util.zip.ZipFile


object TestScalaJsCompiler {

  private val fastCompilationNotMinimized = Optimizer.Fast
  private val charsetName = "UTF-8"
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
}

class TestScalaJsCompiler extends AnyFunSuite:
  testClass =>


  test("Initiate compiler and compile simple code") {
    // Given
    val (compiler, classpath) = createCompilerAndInit

    // When
    val actual = compiler.compileScalaJsString(classpath, TestScalaJsCompiler.scalaJsCode, TestScalaJsCompiler.fastCompilationNotMinimized)

    // Then
    assert(actual != null)
    println("JavaScript length:" + actual.length)
  }
  
  private def createCompilerAndInit: (ScalaJsCompiler, Classpath) = {
    val loader: (String => InputStream) = (jarFile: String) => {
      println(s"Loading file $jarFile")
      new FileInputStream(new File(jarFile))
    }

    val libraryFolder = List("target", "scala-3.3.1", "test-classes").mkString(File.separator) + File.separator
    println(s"Loading libraries from $libraryFolder")
    val libs = Set("scala3-library_3-3.3.1.jar", "scala-library-2.13.10.jar", "scalajs-javalib-1.12.0.jar", "scalajs-library_2.13-1.12.0.jar") // List of all libraries necessary for compilation
    val compiler = ScalaJsCompiler()
    val classpath = compiler.init(loader, libraryFolder, libs)
    (compiler, classpath)
  }