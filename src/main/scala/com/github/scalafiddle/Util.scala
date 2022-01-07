package com.github.scalafiddle

object Util {

  /**
   * Created by haoyi on 2014-06-21
   */
  implicit class Pipeable[T](t : T) {
    def |>[V](f : T => V) : V = f(t)
  }
}
