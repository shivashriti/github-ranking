package com.shivashriti.ranking.utils

import akka.actor.ActorSystem
import akka.http.scaladsl.model.Uri
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.shivashriti.ranking._
import com.shivashriti.ranking.utils.PaginatedApiSource._
import com.shivashriti.ranking.utils.ResourceUtil._

import scala.collection.immutable.Seq
import scala.concurrent.Future

/**
  * Uses dedicated Paginated API Sources to request resources
  */
trait SourceAccessor{
  def repositoriesIn(orgName: String)
                    (implicit system: ActorSystem, materializer: Materializer): Future[Seq[String]] =
    RepoSource(Some(reposByOrgUri(orgName)))
      .requestAllPages

  def contributorsFrom(uris: Seq[Uri])
                      (implicit system: ActorSystem, materializer: Materializer): Future[Seq[Contributor]] =
    Source(uris)
      .mapAsyncUnordered(requestParallelism){ uri =>
        ContributorSource(Some(uri))
          .requestAllPages
      }
      .runFold(Seq[Contributor]())(add)
}