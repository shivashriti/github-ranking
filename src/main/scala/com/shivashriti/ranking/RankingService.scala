package com.shivashriti.ranking

import akka.actor.{ActorRef, ActorSystem}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.util.Timeout
import com.typesafe.config.Config

import com.shivashriti.ranking.actors._
import scala.concurrent.duration._
import scala.concurrent._
import scala.util._

/**
  * Ranking Service starts HTTP Server using config, binds Routes and starts RankingActor to handle queries
  * @param config
  * @param system
  * @param materializer
  */
class RankingService private(config: Config)(implicit system: ActorSystem, materializer: Materializer)
  extends Routes {

  implicit def executor: ExecutionContextExecutor = system.dispatcher
  val logger: LoggingAdapter = Logging(system, getClass)

  override implicit val timeout: Timeout = akka.util.Timeout(60.seconds)

  override val queryHandler: ActorRef = system.actorOf(RankingActor.props)

  val bindingFuture: Future[Http.ServerBinding] =
    Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))

  bindingFuture.onComplete {
    case Success(bound) =>
      logger.info(s"Server Started: ${bound.localAddress.getHostString}")
    case Failure(e) =>
      logger.error(s"Server could not start: ${e.getMessage}")
      system.terminate()
  }
}

object RankingService  {

  def run(config: Config)(implicit system: ActorSystem, materializer: Materializer): RankingService =
    new RankingService(config)
}