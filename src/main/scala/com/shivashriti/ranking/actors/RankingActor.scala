package com.shivashriti.ranking.actors

import akka.actor._
import akka.http.scaladsl.model._
import akka.pattern.pipe
import akka.stream.Materializer
import com.shivashriti.ranking._
import com.shivashriti.ranking.utils.ResourceUtil._
import com.shivashriti.ranking.utils.HttpResponseException
import com.shivashriti.ranking.utils._

import scala.collection.immutable.Seq

/**
  * Ranking Actor when receives organization name in message, uses SourceAccessor to request and collect contributions
  * It gives the collected data in response or propagate errors (if any)
  *
  * @param system
  * @param materializer
  */
class RankingActor(implicit system: ActorSystem, materializer: Materializer) extends Actor with ActorLogging with SourceAccessor {

  import RankingActor._

  override def preStart(): Unit = log.info("RankingActor started")
  override def postStop(): Unit = log.info("RankingActor stopped")

  implicit val ec = context.dispatcher

  override def receive: Receive = {

    case GetContributors(orgName) => {

      log.info(s"[New GetContributors Request for Organization :: ${orgName}]")

      repositoriesIn(orgName)
        .flatMap { repos =>

          log.info(s"[Received repos for Organization :: $orgName]")
          val contributorsUris: Seq[Uri] =
            repos.map( repoName =>
              contributorsByRepoUri(orgName, repoName)
            )

          contributorsFrom(contributorsUris)
            .map { cs =>
              log.info(s"[Received contributors for repos for Organization :: $orgName]")
              val result: ServiceResponse[Seq[Contributor]] = Right(sort(cs))
              GetContributorsResponse(result)
            }
        }
        .recover {
          case e: HttpResponseException => GetContributorsResponse(Left(e.error))
          case e: Exception => GetContributorsResponse(Left(ErrorResponse(e.getMessage, 500)))
        }
        .pipeTo(sender())
    }
  }

}

object RankingActor {

  def props(implicit system: ActorSystem, materializer: Materializer) = Props(classOf[RankingActor], system, materializer)

  case class GetContributors(orgName: String)
  case class GetContributorsResponse(response: ServiceResponse[Seq[Contributor]])

}