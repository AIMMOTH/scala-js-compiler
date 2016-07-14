package fiddle

import org.scalajs.core.tools.io.VirtualScalaJSIRFile

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import java.util.logging.Logger
import org.slf4j.LoggerFactory

sealed abstract class Optimizer

object Optimizer {
  case object Fast extends Optimizer

  case object Full extends Optimizer
}

case class CompileSource(envId: String, templateId: String, sourceCode: String, optimizer: Optimizer)

case class CompleteSource(envId: String, templateId: String, sourceCode: String, flag: String, offset: Int)

class CompileActor(classPath: Classpath, envId: String, templateId: String, sourceCode: String, optimizer: Optimizer) {

  val log = LoggerFactory.getLogger(getClass)
  val compiler = new Compiler(classPath, envId)
  val opt = optimizer match {
    case Optimizer.Fast => compiler.fastOpt _
    case Optimizer.Full => compiler.fullOpt _
  }

  val errorStart = """^\w+.scala:(\d+): *(\w+): *(.*)""".r
  val errorEnd = """ *\^ *$""".r

  def parseErrors(preRows: Int, log: String): Seq[EditorAnnotation] = {
    val lines = log.split('\n').toSeq.map(_.replaceAll("[\\n\\r]", ""))
    val (annotations, _) = lines.foldLeft((Seq.empty[EditorAnnotation], Option.empty[EditorAnnotation])) {
      case ((acc, current), line) =>
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

  def doCompile = {
    compile(compiler, templateId, sourceCode, _ |> opt |> compiler.export)
  }

  private def compile(compiler: Compiler, templateId: String, code: String, processor: Seq[VirtualScalaJSIRFile] => String): CompilerResponse = {
    println(s"Using template $templateId")
    val output = mutable.Buffer.empty[String]

    val res = compiler.compile(templateId, code, output.append(_))
    if (output.nonEmpty)
      println(s"Compiler errors: $output")
    val template = compiler.getTemplate(templateId)

    val preRows = template.pre.count(_ == '\n')
    val logSpam = output.mkString
    log.debug(s"preRows:$preRows")
    log.debug(s"logSpam:$logSpam")
    CompilerResponse(res.map(processor), parseErrors(preRows, logSpam), logSpam)
  }
}
