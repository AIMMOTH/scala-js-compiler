package io.cenet.compiler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "javaServlet", urlPatterns = "/java")
@SuppressWarnings("serial")
public class JavaServlet extends HttpServlet {

  private static Logger logger = LoggerFactory.getLogger(JavaServlet.class);
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    logger.info("Java Servlet callled!");
    ArrayList<Integer> is = new ArrayList<Integer>();
    is.add(3);
    is.add(3);
    PrintWriter out = resp.getWriter();
    out.println("Java 8 Servlet! Distinct of list with 3 and 3 is " + Arrays.toString(is.stream().distinct().toArray()));
  }
}