package io.cenet.compiler

import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServlet

@WebServlet(name = "helloworld", urlPatterns = Array("/scala"), value = Array(""))
@SuppressWarnings(Array("serial"))
class Compiler extends HttpServlet {
  
  override def doGet(request : HttpServletRequest, response : HttpServletResponse) = {
    val out = response.getWriter
    out.println("heja 22")
  }
}