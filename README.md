# Scala JS Compiler
Compiles list of Strings with Scala JS code to a JavaScript.

## Environment
This is a Maven JAR project with Java 8, Scala 2.11 and Scala JS 0.6

## Using it
Class ScalaJsCompiler uses method `init` to create `Classpath` with all necessary libraries loaded.

### Simple Example using Servlet API
This compiler is excellent to use for a backend server compiling JavaScript on request!
```
    @WebListener
    class WebServletContextListener extends ServletContextListener {
    
        val scalaJsCode =
            """
              |package tutorial.webapp
              |
              |object TutorialApp {
              |  def main(args: Array[String]): Unit = {
              |    println("Hello world!")
              |  }
              |}
              |""".stripMargin
              
        def contextInitialized(contextEvent: ServletContextEvent): Unit = {
            val loader: (String => InputStream) = (path) => contextEvent.getServletContext.getResourceAsStream(path)
            
            val compiler = ScalaJsCompiler()
            val classpath = compiler.init(loader, "/WEB-INF/lib/", Set("scala3-library_3-3.3.1.jar", "scala-library-2.13.10.jar", "scalajs-javalib-1.12.0.jar", "scalajs-library_2.13-1.12.0.jar"))
            val javascript = compiler.compileScalaJsString(classpath, scalaJsFile, Optimizer.Fast)
        }
    }
```
This is reading JAR-files in WEB-INF/lib and provided ScalaJS source.
One trick is to put all ScalaJS source in **/src/main/webapp/** to make it available for the Servlet context.