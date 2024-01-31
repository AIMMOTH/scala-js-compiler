# Scala JS Compiler
Compiles list of Strings with Scala JS code to a JavaScript.

## Environment
**Check out new master branch supporting Scala 3.3.1 and Java 21!**

This is a Maven JAR project with Java 8, Scala 2.11 and Scala JS 0.6

**JAVA_HOME** needs to be set to JDK 8!

## Using it
Class ScalaJsCompiler needs to run init to load all dependencies first!
Make sure all dependencies are either with Scala source code or compiled with Scala JS.

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
            val scalaJsFile = ScalaJsFile("Filename", scalaJsSource)
            
            val loader: (String => InputStream) = (path) => contextEvent.getServletContext.getResourceAsStream(path)
            
            val javascript = ScalaJsCompiler().init(loader, "/WEB-INF/lib/")
                .compileScalaJsUtf8StringFast(scalaJsFile)
        }
    }
```
This is reading JAR-files in WEB-INF/lib and provided ScalaJS source.
One trick is to put all ScalaJS source in **/src/main/webapp/** to make it available for the Servlet context.

### Full Example
This example is using a loader that reads local files.
```
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
          
    val compiler = new ScalaJsCompiler
    
    val allJarsOnClassPathSeparated = System.getProperty("java.class.path")
    val separator = System.getProperty("path.separator");
    val jarsWithFullFilePath = allJarsOnClassPathSeparated.split(separator)

    val loader : (String => InputStream)= (jarFile: String) => {
      jarsWithFullFilePath.find(s => s.endsWith(jarFile)) match {
        case Some(found) => {
          println("Found on classpath:" + found)
          new FileInputStream(new File(found))
        }
        case None => throw new FileNotFoundException(jarFile)
      }
    }

    val relativeJarPath = "" // We get full path from above and do not need a relative path
    val additionalLibs = Set[String]("scalajs-dom_sjs0.6_2.11.jar") // This JAR needs to be on classpath (check your Maven dependencies)
    val baseLibs = Seq("scala-library-2.11.12.jar", "scala-reflect-2.11.12.jar", "scalajs-library_2.11-0.6.33.jar") // Always needed for compilation
    compiler.init(loader, relativeJarPath, additionalLibs, baseLibs)
    
    val scalaJsFile = ScalaJsFile("Filename", scalaJsSource)
    
    val fastCompilationNotMinimized = Optimizer.Fast
    val charsetName = "UTF-8"
    val compilerLoggingLevel = Level.Info
  
    val javascript = compiler.compileScalaJsStrings(List(scalaJsFile), fastCompilationNotMinimized, charsetName, compilerLoggingLevel)
```

