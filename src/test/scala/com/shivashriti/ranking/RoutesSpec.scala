package com.shivashriti.ranking

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._

class RoutesSpec extends WordSpec with Matchers with ScalatestRouteTest with Routes {

  val rankingActor = TestProbe()

  override def queryHandler: ActorRef = rankingActor.ref

  override val timeout = akka.util.Timeout(5.seconds)

  "Routes" should {
    val testOrg = "testOrg"

    "query contributors by organization" in {
      val request = HttpRequest(
        method = HttpMethods.GET,
        uri = s"/org/$testOrg/contributors"
      )

      request -> routes -> check {
        status should === (StatusCodes.OK)
      }
    }

  }

}

