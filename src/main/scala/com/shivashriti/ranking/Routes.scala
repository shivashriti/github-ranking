package com.shivashriti.ranking


import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern._
import com.shivashriti.ranking.actors.RankingActor._
import spray.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Routes for the service
  */
trait Routes {

  def queryHandler: ActorRef

  implicit val timeout: akka.util.Timeout
  import Routes._

  val routes: Route =
      path("org" / Segment / "contributors") { orgName =>
        pathEndOrSingleSlash {
          get {
            respondWith(
              (queryHandler ? GetContributors(orgName))
                .mapTo[GetContributorsResponse]
                .map(_.response)
            )
          }
        }
      }
}

object Routes {
  def respondWith[A](response: Future[ServiceResponse[A]])
                    (implicit ee: JsonWriter[ErrorResponse], rr: JsonWriter[A]): StandardRoute =
    complete {
      response.map {
        case Left(error) =>
          HttpResponse(
            status = StatusCodes.custom(error.code, ""),
            entity = HttpEntity(ContentTypes.`application/json`, error.toJson.toString())
          )
        case Right(a) =>
          HttpResponse(
            status = StatusCodes.OK,
            entity = HttpEntity(ContentTypes.`application/json`, a.toJson.toString())
          )
      }
        .recover{
          case e: Exception =>
            e.printStackTrace()
            HttpResponse(status = StatusCodes.InternalServerError)
        }
    }
}
