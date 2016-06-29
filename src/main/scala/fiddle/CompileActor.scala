package fiddle

import akka.actor.{Actor, Props}
import org.scalajs.core.tools.io.VirtualScalaJSIRFile

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

sealed abstract class Optimizer

object Optimizer {
  case object Fast extends Optimizer

  case object Full extends Optimizer
}

case class CompileSource(envId: String, templateId: String, sourceCode: String, optimizer: Optimizer)

case class CompleteSource(envId: String, templateId: String, sourceCode: String, flag: String, offset: Int)

class CompileActor(classPath: Classpath) extends Actor {
  def receive = {
    case CompileSource(envId, templateId, sourceCode, optimizer) =>
      val compiler = new Compiler(classPath, envId)
      val opt = optimizer match {
        case Optimizer.Fast => compiler.fastOpt _
        case Optimizer.Full => compiler.fullOpt _
      }
      sender() ! Try(doCompile(compiler, templateId, sourceCode, _ |> opt |> compiler.export))

    case CompleteSource(envId, templateId, sourceCode, flag, offset) =>
      val compiler = new Compiler(classPath, envId)
      sender() ! Try(Await.result(compiler.autocomplete(templateId, sourceCode, flag, offset.toInt), 30.seconds))
  }

  val errorStart = """^\w+.scala:(\d+): *(\w+): *(.*)""".r
  val errorEnd = """ *\^ *$""".r

  def parseErrors(preRows: Int, log: String): Seq[EditorAnnotation] = {
    val lines = log.split('\n').toSeq.map(_.replaceAll("[\\n\\r]", ""))
    val (annotations, _) = lines.foldLeft((Seq.empty[EditorAnnotation], Option.empty[EditorAnnotation])) { case ((acc, current), line) =>
      line match {
        case errorStart(lineNo, severity, msg) =>
          val ann = EditorAnnotation(lineNo.toInt - preRows - 1, 0, Seq(msg), severity)
          (acc, Some(ann))
        case errorEnd() if current.isDefined =>
          val ann = current.map(ann => ann.copy(col = line.length, text = ann.text :+ line)).get
          (acc :+ ann, None)
        case errLine =>
          (acc, current.map(ann => ann.copy(text = ann.text :+ errLine)))
      }
    }
    annotations
  }

  def doCompile(compiler: Compiler, templateId: String, code: String, processor: Seq[VirtualScalaJSIRFile] => String): CompilerResponse = {
    println(s"Using template $templateId")
    val output = mutable.Buffer.empty[String]

    val res = compiler.compile(templateId, code, output.append(_))
    if(output.nonEmpty)
      println(s"Compiler errors: $output")
    val template = compiler.getTemplate(templateId)

    val preRows = template.pre.count(_ == '\n')
    val logSpam = output.mkString
    CompilerResponse(res.map(processor), parseErrors(preRows, logSpam), logSpam)
  }
}

object CompileActor {
  def props(classPath: Classpath) = Props(new CompileActor(classPath))
}