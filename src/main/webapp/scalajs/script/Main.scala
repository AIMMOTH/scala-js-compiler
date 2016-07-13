package script

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
