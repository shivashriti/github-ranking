package com.shivashriti.ranking

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.shivashriti.ranking.actors.RankingActor
import com.shivashriti.ranking.actors.RankingActor._
import com.shivashriti.ranking.utils.HttpResponseException._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.collection.immutable.Seq
import scala.concurrent.Future

class RankingActorSpec
  extends TestKit(ActorSystem("RankingActorSpec"))
  with ImplicitSender
  with WordSpecLike
  with BeforeAndAfterAll {

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val rankingActor = TestActorRef(new RankingActor{
    override def repositoriesIn(orgName: String)(implicit system: ActorSystem, materializer: Materializer): Future[Seq[String]] =
      orgName match {
        case "testOrg1" => Future.successful (Seq ("testRepo1", "testRepo1") )
        case "testOrg2" => Future (throw ResourceNotFoundException)
        case "testOrg3" => Future (throw ForbiddenException)
      }

    override def contributorsFrom(uris: Seq[Uri])(implicit system: ActorSystem, materializer: Materializer): Future[Seq[Contributor]] =
      Future.successful(Seq(Contributor("John", 10), Contributor("Mary", 8), Contributor("Steve", 20)))
  })

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Ranking Actor" must {

    "send total contributors sorted by number for all repos of org given in message" in {

      rankingActor ! GetContributors("testOrg1")

      expectMsg(
        GetContributorsResponse(Right(Seq(Contributor("Steve", 20), Contributor("John", 10), Contributor("Mary", 8))))
      )
    }

    "send NotFound message and 404 code for unknown org" in {

      rankingActor ! GetContributors("testOrg2")

      expectMsg(
        GetContributorsResponse(Left(ErrorResponse(ResourceNotFoundException.error.message, 404)))
      )
    }

    "send Forbidden message and 403 code for forbidden resource" in {

      rankingActor ! GetContributors("testOrg3")

      expectMsg(
        GetContributorsResponse(Left(ErrorResponse(ForbiddenException.error.message, 403)))
      )
    }

  }

}
