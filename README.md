Scala JS Compiler
=================
Compiles list of Strings with Scala JS code to a JavaScript. Use this by reading your Scala JS source code and let backend respond with a JavaScript.

Using it Live
-------------
If this compiler is used live, use it as a dependency and make sure you find the dependency JAR files with a relative path. Usually it's something like "/WEB-INF/lib/". Check out the "web-demo" branch for a live demo. Make sure all dependencies are either with Scala source code or compiled with Scala JS.

Class Loader
------------
This Scala JS Compiler uses a ClassLoader. If you want to use ServletContext, switch to branch "servlet-compiler".

Test
----
There's a unit test loading necessary dependencies and a extra one. The test is using the following JAR files put in 'src/test/resources':
 * scalajs-dom_sjs0.6_2.11-0.9.0.jar
 * scalajs-library_2.11-0.6.9.jar
 * scala-library-2.11.8.jar
 * scala-reflect-2.11.8.jar

Environment
-----------
This is a Maven JAR project with Java 8, Scala and Scala JS.

Live
----
It's used in a web demo. Check it out [here](https://scala-js-compiler.appspot.com/index.scala). It uses the branch [web-demo](https://github.com/AIMMOTH/scala-js-compiler/tree/web-demo).

