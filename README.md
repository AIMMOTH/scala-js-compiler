# Scala JS Compiler
Compiles list of Strings with Scala JS code to a JavaScript.

## Environment
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
    
        def contextInitialized(contextEvent: ServletContextEvent): Unit = {
            val loader: (String => InputStream) = (path) => contextEvent.getServletContext.getResourceAsStream(path)
            
            val javascript = ScalaJsCompiler().init(loader, "/WEB-INF/lib/")
                .compileScalaJsUtf8StringFast(scalaJsCode)
        }
    }
```
This is reading JAR-files in WEB-INF/lib and provided ScalaJS source.
One trick is to put all ScalaJS source in **/src/main/webapp/** to make it available for the Servlet context.

### Full Example
This example is using a loader that reads local files.
```
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
    val additionalLibs = Set[String]() // We don't have any extra dependencies in this repository
    val baseLibs = Seq("scala-library-2.11.12.jar", "scala-reflect-2.11.12.jar", "scalajs-library_2.11-0.6.33.jar") // Always needed for compilation
    compiler.init(loader, relativeJarPath, additionalLibs, baseLibs)
    
    val javascript = compiler.compileScalaJsStrings(List(scalaJsCode), fastCompilationNotMinimized, charsetName, compilerLoggingLevel)
```

## Live
Scala Stack Angular is using this to compile AngularJS code and create a whole app including POST and GET to backend.
Check it out [here](https://scala-stack-angular.appspot.com/) (takes a few minutes to load).

## Links
* https://github.com/AIMMOTH/scala-js-compiler
* https://github.com/AIMMOTH/scala-stack-angular (implementation with AngularJS)
* https://scala-stack-angular.appspot.com/ (live demo)
* https://github.com/scalafiddle 
* https://github.com/lihaoyi/scala-js-fiddle 