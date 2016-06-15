package io.cenet.compiler

import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServlet

@WebServlet(name = "scalaServlet", urlPatterns = Array("/scala"))
@SuppressWarnings(Array("serial"))
class Compiler extends HttpServlet {
  
  val x = 3
  
  override def doGet(request : HttpServletRequest, response : HttpServletResponse) = {
    val out = response.getWriter
    val list = 1 to x
    out.println(s"Scala! Sum of 1 to $x is ${list.sum}")
  }
}