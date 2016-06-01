package io.cenet.compiler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "helloworld", urlPatterns = {"/hello"}, value = "")
@SuppressWarnings("serial")
public class TestServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    ArrayList<Integer> is = new ArrayList<Integer>();
    is.add(3);
    is.add(3);
    PrintWriter out = resp.getWriter();
    out.println("Hello, world 22 " + Arrays.toString(is.stream().distinct().toArray()));
  }
}