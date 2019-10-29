package com.shivashriti

import akka.http.scaladsl.common.{EntityStreamingSupport, JsonEntityStreamingSupport}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._

import scala.collection.immutable.Seq

/**
  * Defines ADTs, constants, encoders/decoders used in entire application
  */
package object ranking extends SprayJsonSupport with DefaultJsonProtocol {

  type ServiceResponse[T] = Either[ErrorResponse, T]

  case class ErrorResponse(message: String, code: Int)

  case class Contributor(name: String, contributions: Int)

  def sort(cs: Seq[Contributor]): Seq[Contributor] =
    cs.sortWith((c1, c2) => c1.contributions > c2.contributions)

  def add(s1: Seq[Contributor], s2: Seq[Contributor]): Seq[Contributor] =
    s1
      .foldLeft(toMap(s2)){
        case (map, c) => if(map.contains(c.name))
          map.+(c.name -> (map.get(c.name).get + c.contributions))
        else map.+(c.name -> c.contributions)
      }
      .map(c => Contributor(c._1, c._2))
      .toList

  def toMap(cs: Seq[Contributor]): Map[String, Int] =
    cs.foldLeft(Map[String, Int]())((map, c) => map.+(c.name -> c.contributions))

  implicit val contributorFormat = jsonFormat2(Contributor)
  implicit val errorFormat = jsonFormat2(ErrorResponse)
  implicit val jsonStreamingSupport: JsonEntityStreamingSupport = EntityStreamingSupport.json()

  val authToken = sys.env.get("GH_TOKEN")
  val requestParallelism = 10
  val entitiesPerPage = 100
}
