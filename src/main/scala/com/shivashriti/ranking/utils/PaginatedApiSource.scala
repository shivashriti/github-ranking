package com.shivashriti.ranking.utils

import java.util.concurrent.Executors

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.Materializer
import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Keep, Sink, Source}
import com.shivashriti.ranking.utils.ResourceUtil._
import com.shivashriti.ranking.utils.HttpResponseException._
import com.shivashriti.ranking._

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
  * Paginated API Source Interface
  *
  * Given a source for querying a REST api page by page and accumulating data T from responses
  * using the add functionality given over T, provides a way to request accumulated data from all pages of API
  *
  * @tparam T
  */
private[ranking] trait PaginatedApiSource[T]{

  val source: Source[Seq[T], NotUsed]

  def add: (Seq[T], Seq[T]) => Seq[T]

  def requestAllPages(implicit materializer: Materializer): Future[Seq[T]] =
    source
      .toMat(Sink.fold[Seq[T], Seq[T]](Nil)(add))(Keep.right)
      .run()
}


object PaginatedApiSource {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50))

  /**
    * Creates a PaginatedApiSource
    *
    * @param initialUri   Uri of initial page
    * @param extractF     way to extract required data from HTTP response
    * @param addF         way to add data extracted from pages
    * @param system
    * @param materializer
    * @tparam T
    * @return
    */
  def apply[T](initialUri: Option[Uri],
               extractF: ResponseEntity => Future[Seq[T]],
               addF: (Seq[T], Seq[T]) => Seq[T])
              (implicit system: ActorSystem,
               materializer: Materializer): PaginatedApiSource[T] = new PaginatedApiSource[T] {

    // unfoldAsync takes uri of first page, extracts next page uri and keeps collecting the data till last page
    val source = Source.unfoldAsync(initialUri) { firstPageUri =>
      firstPageUri match {
        case None => Future.successful(None)
        case Some(uri) =>
          val request = requestFromUri(uri)
          Http()
            .singleRequest(request)
            .flatMap { response =>
              val (currentPageData, nextPageUri) = collectResponseDataAndNextUri(response, extractF)
              currentPageData.map{ data =>
                Some(nextPageUri, data)
              }
            }
          .recover{
            case e: HttpResponseException => throw e
            case _ => throw InternalErrorException
          }
      }
    }

    def add = addF
  }

  /**
    * Paginated API Source to collect contributors
    *
    * @param initialUri     first page uri to request contributors for a repo
    * @param system
    * @param materializer
    * @return
    */
  def ContributorSource(initialUri: Option[Uri])
                       (implicit system: ActorSystem,
                        materializer: Materializer): PaginatedApiSource[Contributor] =

    PaginatedApiSource(initialUri, extractContributors, add)

  /**
    * Paginated API Source to collect a specific string field from response entity
    *
    * @param initialUri     first page uri
    * @param fieldName
    * @param system
    * @param materializer
    * @return
    */
  def TextSource(initialUri: Option[Uri], fieldName: String)
                (implicit system: ActorSystem,
                 materializer: Materializer): PaginatedApiSource[String] =

    PaginatedApiSource(
      initialUri,
      extractField(fieldName),
      (s1, s2) => s1 ++ s2
    )

  /**
    * Paginated API Source to collect repository names within one organization
    *
    * @param initialUri     first page uri to request repos of organization
    * @param system
    * @param materializer
    * @return
    */
  def RepoSource(initialUri: Option[Uri])
                (implicit system: ActorSystem,
                 materializer: Materializer): PaginatedApiSource[String] =

    TextSource(initialUri, "name")


  private def collectResponseDataAndNextUri[T](response: HttpResponse,
                                                f: ResponseEntity => Future[Seq[T]])
                                               (implicit materializer: Materializer): (Future[Seq[T]], Option[Uri]) =

    response match {
      case HttpResponse(StatusCodes.OK, headers, entity, _) =>
        val nextUri = collectNextUri(headers)
        val currentPageData = f(entity)
        (currentPageData, nextUri.headOption)

      case HttpResponse(StatusCodes.NoContent, _, _, _) =>
        (Future.successful(Nil), None)

      case HttpResponse(StatusCodes.NotFound, _, _, _) =>
        throw ResourceNotFoundException

      case HttpResponse(StatusCodes.Forbidden, _, _, _) =>
        throw ForbiddenException

      case HttpResponse(_, _, _, _) =>
        throw UnknownResponseException
    }

}

