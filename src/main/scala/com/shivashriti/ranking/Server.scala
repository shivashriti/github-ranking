package com.shivashriti.ranking

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Server is the entry point of application
  * It runs Ranking Service
  */
object Server extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = ActorMaterializer()

  def config: Config = ConfigFactory.load()

  RankingService.run(config)
}
