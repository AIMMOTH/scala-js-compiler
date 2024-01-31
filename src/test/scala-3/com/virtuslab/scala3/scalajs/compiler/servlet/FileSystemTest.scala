package com.aimmoth.scalajs.compiler.servlet

import dotty.tools.dotc.classpath.VirtualDirectoryClassPath
import dotty.tools.io.VirtualDirectory
import org.scalatest.funsuite.AnyFunSuite

import java.io.File

class FileSystemTestextends extends AnyFunSuite:
  testClass =>

    class MyVirtualDirectoryClassPath(dir: VirtualDirectory) extends VirtualDirectoryClassPath(dir) {
      override def getSubDir(path: String) = super.getSubDir(path)
    }

    /**
     * This test does not work in Windows
     * https://github.com/lampepfl/dotty/discussions/19568 
     */
    test("Virtual directory") {
        val directory = new VirtualDirectory("in memory", None)
        val virtualDirectory = new MyVirtualDirectoryClassPath(directory)

        virtualDirectory.getSubDir("scala\\")
      }