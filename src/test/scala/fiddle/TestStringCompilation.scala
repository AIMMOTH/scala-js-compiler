package fiddle

import org.junit.Test
import org.junit.Assert
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import scala.reflect.io.Streamable

class TestStringCompilation {
  
  val source = 
      """
      package example
      
      import scala.scalajs.js
      import js.annotation._
      import org.scalajs.dom
      
      @JSExport
      class Foo(val x: Int) {
      override def toString(): String = s"Foo($x)"
      }
      
      @JSExportAll
      object HelloWorld extends js.JSApp {
      
      def main(): Unit = {
      println("Hello world!")
      }
      
      def alert = dom.window.alert("Hello!")
      }
      """
  
//  @Test
  def compileStringFast : Unit = {
    // Use directory relative to "scala-js-compiler/target/test-classes/fiddle"
    val classpath = Classpath(getClass, "../")
    val actor = new CompileActor(classpath, "scalatags", "raw", source, Optimizer.Fast)
    actor.doCompile match {
      case cr if cr.jsCode.isDefined =>
        println(cr.jsCode.get)
      case cr =>
        throw new Exception(cr.log)
    }
  }  
//  @Test
  def compileStringFull : Unit = {
    // Use directory relative to "scala-js-compiler/target/test-classes/fiddle"
    val classpath = Classpath(getClass, "../")
    val actor = new CompileActor(classpath, "scalatags", "raw", source, Optimizer.Full)
    actor.doCompile match {
      case cr if cr.jsCode.isDefined =>
        println(cr.jsCode.get)
      case cr =>
        throw new Exception(cr.log)
    }
  }
  
  @Test
  def compileJarWithSource : Unit = {
    val input = getClass.getResourceAsStream("../scala-js-source-1.0-SNAPSHOT-sources.jar")
    val zip = new ZipInputStream(input)
    val sourceFiles = Iterator.continually({
        try {
          zip.getNextEntry
        } catch {
          case e : Throwable =>
            null
        }
      }).takeWhile(_ != null)
      .filter(_.getName.endsWith(".scala"))
      .map(_ => new String(Streamable.bytes(zip))).toList
    
    val classpath = Classpath(getClass, "../")
    val actor = new CompileActor(classpath, "scalatags", "raw", source, Optimizer.Full)
    actor.doCompile match {
      case cr if cr.jsCode.isDefined =>
        println(cr.jsCode.get)
      case cr =>
        throw new Exception(cr.log)
    }
  }
}