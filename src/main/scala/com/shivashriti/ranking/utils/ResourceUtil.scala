package com.shivashriti.ranking.utils

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import com.shivashriti.ranking._
import spray.json.JsObject

import scala.collection.immutable.Seq
import scala.concurrent.Future


object ResourceUtil {

  // prepares uri to fetch repositories from a GitHub organization
  def reposByOrgUri(orgName: String): Uri =
    Uri(s"https://api.github.com/orgs/$orgName/repos?per_page=$entitiesPerPage")

  // prepares uri to fetch contributors from a GitHub Repository and Organization
  def contributorsByRepoUri(orgName: String, repoName: String): Uri =
    Uri(s"https://api.github.com/repos/$orgName/$repoName/contributors?per_page=$entitiesPerPage")

  // makes a stream of the data of Response entity
  def byteSource(entity: ResponseEntity)(implicit materializer: Materializer) =
    entity.dataBytes
      .via(jsonStreamingSupport.framingDecoder)
      .mapAsyncUnordered(10)(bytes => Unmarshal(bytes).to[JsObject])

  // extracts Contributors from Response entity
  def extractContributors(entity: ResponseEntity)(implicit materializer: Materializer): Future[Seq[Contributor]] =
    byteSource(entity)
      .runFold(Seq[Contributor]())((contributors, data) => {
        val loginOption = data.fields.get("login")
        val numOfContributions = data.fields.get("contributions")

        (loginOption, numOfContributions) match {
          case (Some(login), Some(n)) =>
            Contributor(login.convertTo[String], n.convertTo[Int]) +: contributors
          case _ => contributors
        }
      })

  // extracts text field from Response entity
  def extractField(fieldName: String)(entity: ResponseEntity)
                  (implicit materializer: Materializer): Future[Seq[String]] =
    byteSource(entity)
      .runFold(Seq[String]())((repoList, data) => {
        data.fields.get(fieldName) match {
          case Some(repo) => repo.convertTo[String] +: repoList
          case _ => repoList
        }
      })

  // makes an HTTP request from given GitHub uri attaching the token in header
  def requestFromUri(requestUri: Uri): HttpRequest =
    HttpRequest(
      method = HttpMethods.GET,
      uri = requestUri,
      headers = if(authToken.isDefined) Seq(Authorization(OAuth2BearerToken(authToken.get))) else Nil
    )

  // extracts Link for next pag uri from the headers of HTTP response
  def collectNextUri(headers: Seq[HttpHeader]): Seq[Uri] =
    headers.collect {
      case Link(ls) =>
        ls.collect {
          case LinkValue(uri, params) if params.filter(_ == LinkParams.next).nonEmpty => uri
        }
    }.flatten
}
