package fiddle

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.CacheDirectives.`max-age`
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.routing.FromConfig
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
import org.slf4j.LoggerFactory
import upickle.default._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.scalajs.niocharset.StandardCharsets
import scala.util.{Failure, Success, Try}

object Server extends App {
  implicit val system = ActorSystem()
  implicit val timeout = Timeout(30.seconds)
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher
  val log = LoggerFactory.getLogger(getClass)
  // initialize classpath singleton, loads all libraries
  val classPath = new Classpath

  // create compiler router
  val compilerRouter = system.actorOf(FromConfig.props(CompileActor.props(classPath)), "compilerRouter")

  import HttpCharsets._
  import MediaTypes._
  val route = {
    encodeResponse {
      get {
        path("embed") {
          respondWithHeaders(Config.httpHeaders) {
            // main embedded page can be cached for some time (1h for now)
            respondWithHeader(`Cache-Control`(`max-age`(60L * 60L * 1L))) {
              parameterMap { paramMap =>
                complete {
                  HttpEntity(
                    `text/html` withCharset `UTF-8`,
                    Static.renderPage(
                      Config.clientFiles,
                      paramMap
                    )
                  )
                }
              }
            }
          }
        } ~ path("compile") {
          // compile results can be cached for a long time (24h for now)
          respondWithHeader(`Cache-Control`(`max-age`(60L * 60L * 24L))) {
            parameters('source, 'opt, 'template, 'env) { (source, opt, template, env) =>
              ctx =>
                val optimizer = opt match {
                  case "fast" => Optimizer.Fast
                  case "full" => Optimizer.Full
                  case _ =>
                    throw new IllegalArgumentException(s"$opt is not a valid opt value")
                }
                val res = ask(compilerRouter, CompileSource(env, template, decodeSource(source), optimizer))
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
                ctx.complete(res)
            }
          }
        } ~ path("complete") {
          // code complete results can be cached for a long time (24h for now)
          respondWithHeader(`Cache-Control`(`max-age`(60L * 60L * 24L))) {
            parameters('source, 'flag, 'offset, 'template, 'env) { (source, flag, offset, template, env) =>
              ctx =>
                val res = ask(compilerRouter, CompleteSource(env, template, decodeSource(source), flag, offset.toInt))
                  .mapTo[Try[List[(String, String)]]]
                  .map {
                    case Success(cr) =>
                      val result = write(cr)
                      HttpResponse(StatusCodes.OK, entity = HttpEntity(`application/json`, result))
                    case Failure(ex) =>
                      log.error("Error in tab completion", ex)
                      HttpResponse(StatusCodes.BadRequest, entity = ex.getMessage.take(64))
                  }
                ctx.complete(res)
            }
          }
        } ~ path("cache" / Segment) { res =>
          // resources identified by a hash can be cached "forever" (a year in this case)
          respondWithHeader(`Cache-Control`(`max-age`(60L * 60L * 24L * 365))) {
            complete {
              val (hash, ext) = res.span(_ != '.')
              val contentType: ContentType = ext match {
                case ".css" => `text/css` withCharset `UTF-8`
                case ".js" => `application/javascript` withCharset `UTF-8`
                case _ => `application/octet-stream`
              }
              Static.fetchResource(hash) match {
                case Some(src) =>
                  HttpResponse(StatusCodes.OK, entity = HttpEntity(contentType, src))
                case None =>
                  HttpResponse(StatusCodes.NotFound)
              }
            }
          }
        } ~ path("parse") {
          parameters('source, 'template, 'env) { (source, templateId, envId) =>
            complete {
              val dSrc = decodeSource(source)
              val fullSource = Config.templates.get(templateId).map(_.fullSource(dSrc)).getOrElse(dSrc)
              val libs = Config.environments.getOrElse(envId, Nil).flatMap(lib => Config.extLibs.get(lib))
              val finalSource = fullSource + "\n// Libraries:\n" + libs.mkString("// ", "\n// ", "\n")
              HttpResponse(StatusCodes.OK, entity = HttpEntity(`text/plain` withCharset `UTF-8`, ByteString(finalSource)))
            }
          }
        } ~ getFromResourceDirectory("/web")
      }
    }
  }

  def decodeSource(b64: String): String = {
    import com.github.marklister.base64.Base64._
    implicit def scheme: B64Scheme = base64Url
    new String(Decoder(b64).toByteArray, StandardCharsets.UTF_8)
  }

  println(s"Scala Fiddle ${Config.version}")

  // start the HTTP server
  val bindingFuture = Http().bindAndHandle(route, Config.interface, Config.port)
}
