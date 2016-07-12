package io.cenet

import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServlet
import scalatags.Text.all._

@WebServlet(name = "htmlBuilder", urlPatterns = Array("/index.scala"))
class HtmlBuilder extends HttpServlet {
  
  val logger = LoggerFactory.getLogger(getClass);
    
  override def doGet(request : HttpServletRequest, response : HttpServletResponse) = {
    response.getWriter.println(html(
      head(
        script(src:="javascript.js")
      ),
      body(
        div(
          h1(id:="title", "This is a title"),
          p("This is a big paragraph of text"),
          button(onclick:="example.HelloWorld().alert()")("Run Virtual Compiled ScalaJS")
        )
      )
    ).toString)
  }
}