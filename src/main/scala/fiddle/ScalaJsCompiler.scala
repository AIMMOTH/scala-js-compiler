package fiddle

import scala.language.postfixOps

import org.slf4j.LoggerFactory

class ScalaJsCompiler {

  val log = LoggerFactory.getLogger(getClass)
  
  def compile(source : String, optimizer : Optimizer, relativeJarPath : String) : String = {

    val actor = new CompileActor(Classpath(getClass, relativeJarPath), "scalatags", "raw", source, optimizer)
    actor.doCompile match {
      case cr if cr.jsCode.isDefined =>
        cr.jsCode.get
      case cr =>
        throw new Exception(cr.log)
    }
  }
}