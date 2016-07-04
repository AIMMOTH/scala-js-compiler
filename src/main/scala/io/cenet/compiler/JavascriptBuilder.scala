package io.cenet.compiler

import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import java.util.logging.Logger

@WebServlet(name = "javascriptServlet", urlPatterns = Array("/javascript.js"))
@SuppressWarnings(Array("serial"))
class JavascriptBuilder extends HttpServlet {

  private val log = Logger.getLogger(classOf[JavascriptBuilder].getName())

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {

    val script = VirtualSjsCompiler().getOrElse("""//empty""")

    log.info(script)

    response.getWriter().print(script)
  }
}