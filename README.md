Scala JS Compiler
=================
Compiles list of Strings with Scala JS code to a JavaScript. Use this by reading your Scala JS source code and let backend respond with a JavaScript.

Using it Live
-------------
If this compiler is used live, use it as a dependency and make sure you find the dependency JAR files with a relative path. Usually it's something like "/WEB-INF/lib/". Check out the "web-demo" branch for a live demo.

Jetty
-----
This Scala JS Compiler uses ServletContext to load classes and SLF4J for logging.

Environment
-----------
This is a Maven project with Google Flexible Environment with Java 8, Scala 2.11 and 
Web Servlets 3.1.

Installation
------------
Install Java 8, Scala 2.11 and Maven 3.3.

Run and Deploy
--------------

Use maven to build with $ mvn clean package install 