Google Flexible Environment with Scala
======================================
This is a Maven project with Google Flexible Environment with Java 8, Scala and 
Web Servlets 3.1.

Installation
------------
Install Java 8, Maven 3.3 and Google Cloud

Run Locally
-----------
Use Python in Google Cloud to run the following script in project directory
Build project with either maven
```
$ cd project
$ mvn package
$ \gcloud_sdk\google-cloud-sdk\platform\bundledpython\python.exe \gcloud_sdk\google-cloud-sdk\bin\dev_appserver.py target\scala-js-compiler-1.0-SNAPSHOT  
```
Or build project to src/main/webapp with an IDE: 
```
$ cd project
$ \gcloud_sdk\google-cloud-sdk\platform\bundledpython\python.exe \gcloud_sdk\google-cloud-sdk\bin\dev_appserver.py src\main\webapp
```
