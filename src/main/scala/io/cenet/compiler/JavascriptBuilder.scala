package io.cenet.compiler

import scalatags.stylesheet.Sheet
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import java.util.logging.Logger

class JavascriptBuilder extends HttpServlet {

  private val log = Logger.getLogger(classOf[JavascriptBuilder].getName())

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {

    val script = VirtualSjsCompiler().getOrElse("""//empty""")

    log.info(script)

    response.getWriter().print(script)
  }
}