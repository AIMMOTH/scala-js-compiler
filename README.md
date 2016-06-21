Google Flexible Environment with Scala
======================================
This is a Maven project with Google Flexible Environment with Java 8, Scala and 
Web Servlets 3.1.

Live
----
Check it out [here](https://20160610t190610-dot-scala-js-compiler.appspot.com/)

Installation
------------
Install Java 8, Maven 3.3 and Google Cloud. Important to have Python on path to 
make Google Cloud to work!

Run Locally
-----------

Either use maven to build and run with $ mvn gcloud:run

Or use GCloud development server which is bundled with the GCloud SDK. Use the bundled python and run the follwing (including debugging on port 9000)

$ python %GCLOUD_SDK_HOME%\bin\dev_appserver.py src\main\webapp --jvm_flag=-agentlib:jdwp=transport=dt_socket,address=9000,server=y,suspend=n

Deploy
------ 

Use maven goal $ mvn gcloud:deploy