package fiddle

import scala.language.postfixOps

import org.slf4j.LoggerFactory

import javax.servlet.ServletConfig
import javax.servlet.ServletContext
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebServlet(name = "fiddleServlet", urlPatterns = Array("/javascript.js"))
class FiddleServlet extends HttpServlet {

  val log = LoggerFactory.getLogger(getClass)
  val source = """

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
  
  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {

    val optimizer = request.getParameter("optimizer") match {
      case "full" => Optimizer.Full
      case _      => Optimizer.Fast
    }

    val actor = new CompileActor(Classpath(request.getServletContext), "scalatags", "raw",
      source, optimizer)
    actor.doCompile match {
      case cr if cr.jsCode.isDefined =>
        response.setHeader("content-type", "application/json")
        response.getWriter.println(cr.jsCode.get)
        response.getWriter.flush
      case cr =>
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, cr.log)
    }
  }
}