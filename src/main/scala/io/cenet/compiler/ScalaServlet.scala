package io.cenet.compiler

import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServlet
import org.slf4j.LoggerFactory

@WebServlet(name = "scalaServlet", urlPatterns = Array("/scala"))
@SuppressWarnings(Array("serial"))
class Compiler extends HttpServlet {
  
  val logger = LoggerFactory.getLogger(classOf[Compiler]);
  val x = 3
  
  override def doGet(request : HttpServletRequest, response : HttpServletResponse) = {

    logger.info("haaaj")
    val out = response.getWriter
    val list = 1 to x
    out.println(s"Scala! Sum of 1 to $x is ${list.sum}")
  }
}