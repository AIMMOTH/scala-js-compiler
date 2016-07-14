Scala JS Compiler
=================
Compiles list of Strings with Scala JS code to a JavaScript. Use this by reading your Scala JS source code and let backend respond with a JavaScript.

Using it Live
-------------
If this compiler is used live, use it as a dependency and make sure you find the dependency JAR files with a relative path. Usually it's something like "/WEB-INF/lib/". Check out the "web-demo" branch for a live demo.

Test
----
Make sure you have the following JAR files in 'src/test/resources' (they are all dependencies):
 * scalajs-dom_sjs0.6_2.11-0.9.0.jar
 * scalajs-library_2.11-0.6.9.jar
 * scala-library-2.11.8.jar
 * scala-reflect-2.11.8.jar
 * scalatags_2.11-0.5.4.jar

Environment
-----------
This is a Maven project with Google Flexible Environment with Java 8, Scala and 
Web Servlets 3.1.

Live
----
Check it out [here](https://scala-js-compiler.appspot.com/index.scala). It uses the branch [web-demo](https://github.com/AIMMOTH/scala-js-compiler/tree/web-demo).

Installation
------------
Install Java 8, Maven 3.3 and Google Cloud. Important to have Python on path to 
make Google Cloud to work!

Run and Deploy
--------------

Use maven and run $ mvn gcloud:run or $ mvn gcloud:deploy. Make sure Python is on
path!
