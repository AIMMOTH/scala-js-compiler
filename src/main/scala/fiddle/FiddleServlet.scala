package fiddle

import akka.actor.{ ActorSystem, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.CacheDirectives.`max-age`
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.routing.FromConfig
import akka.stream.ActorMaterializer
import akka.util.{ ByteString, Timeout }
import org.slf4j.LoggerFactory
import upickle.default._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.scalajs.niocharset.StandardCharsets
import scala.util.{ Failure, Success, Try }
import org.slf4j.LoggerFactory
import javax.servlet.annotation.WebServlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.ServletContext
import javax.servlet.ServletConfig

@WebServlet(name = "fiddleServlet", urlPatterns = Array("/fiddle.js"))
class FiddleServlet extends HttpServlet {

  implicit val system = ActorSystem()
  implicit val timeout = Timeout(30.seconds)
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  val log = LoggerFactory.getLogger(classOf[FiddleServlet])
  // initialize classpath singleton, loads all libraries
  var context : ServletContext = null

  // create compiler router

  import HttpCharsets._
  import MediaTypes._

  override def doGet(request: HttpServletRequest, response: HttpServletResponse) = {
    val opt = "fast"
    val optimizer = opt match {
      case "fast" => Optimizer.Fast
      case "full" => Optimizer.Full
      case _ =>
        throw new IllegalArgumentException(s"$opt is not a valid opt value")
    }
    val source = """"""
  val classPath = new Classpath(context)
    val compilerRouter = system.actorOf(FromConfig.props(CompileActor.props(classPath)), "compilerRouter")
    val res = ask(compilerRouter, CompileSource("scalatags", "raw", decodeSource(source), optimizer))
      .mapTo[Try[CompilerResponse]]
      .map {
        case Success(cr) =>
          val result = write(cr)
          HttpResponse(StatusCodes.OK, entity = HttpEntity(`application/json`, ByteString(result)))
        case Failure(ex) =>
          HttpResponse(StatusCodes.BadRequest, entity = ex.getMessage.take(64))
      } recover {
        case e: Exception =>
          log.error("Error in compilation", e)
          HttpResponse(StatusCodes.InternalServerError)
      }
  }
  
  override def init(config: ServletConfig) = {
    context = config.getServletContext
  }

  def decodeSource(b64: String): String = {
    import com.github.marklister.base64.Base64._
    implicit def scheme: B64Scheme = base64Url
    new String(Decoder(b64).toByteArray, StandardCharsets.UTF_8)
  }
}