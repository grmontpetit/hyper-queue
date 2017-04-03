/*
 * Copyright 2017 Gabriel Robitaille-Montpetit (grmontpetit@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.grmontpetit.apis

import com.grmontpetit.model.exceptions.{HyperQueueException, TopicNotFoundException}
import com.grmontpetit.model.JsonModelObject._
import com.grmontpetit.model.data.ModelObject
import spray.http.HttpHeaders.RawHeader
import spray.http.MediaTypes._
import spray.http.{HttpEntity, HttpHeader, HttpResponse, StatusCodes}
import spray.http.StatusCodes._
import spray.json._
import spray.routing.{HttpService, Route, StandardRoute}

import scala.util.{Failure, Success, Try}

object HyperQueueApi {

  trait HyperQueueResponseSuccess {
    def marshal(): HttpResponse
  }
  trait HyperQueueResponseFailure extends Exception {
    def marshal(): HttpResponse
  }

  case class ItemConsumed(item: ModelObject) extends HyperQueueResponseSuccess {
    def marshal() = HttpResponse(StatusCodes.OK, entity = HttpEntity(`application/json`, item.toJson.toString))
  }
  case class ItemProduced(item: ModelObject) extends HyperQueueResponseSuccess {
    def marshal() = HttpResponse(StatusCodes.Created, entity = HttpEntity(`application/json`, item.toJson.toString))
  }
  case class ItemsInfo(items: List[ModelObject]) extends HyperQueueResponseSuccess {
    val json = JsObject("items" -> items.toJson, "total" -> JsNumber(items.size))
    def marshal() = HttpResponse(StatusCodes.OK, entity = HttpEntity(`application/json`, items.toJson.toString))
  }
  case class ItemInfo(item: ModelObject) extends HyperQueueResponseSuccess {
    def marshal() = HttpResponse(StatusCodes.OK, entity = HttpEntity(`application/json`, item.toJson.toString()))
  }
  case class ItemError(item: ModelObject) extends HyperQueueResponseFailure {
    def marshal() = HttpResponse(StatusCodes.InternalServerError, entity = HttpEntity(`application/json`, item.toJson.toString()))
  }
  case class ItemTimeout(item: ModelObject) extends HyperQueueResponseFailure {
    def marshal() = HttpResponse(StatusCodes.NetworkConnectTimeout, entity = HttpEntity(`application/json`, item.toJson.toString()))
  }
  case class ConsumerAccepted(id: Int) extends HyperQueueResponseSuccess {
    val h = RawHeader("id", id.toString)
    def marshal() = HttpResponse(StatusCodes.Accepted, headers = List(h))
  }

}

abstract class HyperQueueApi extends HttpService {

  import com.grmontpetit.apis.HyperQueueApi._
  def routes: Route

  val futureHandler: PartialFunction[Try[Any], StandardRoute] = {
    case Success(response: HyperQueueResponseSuccess) =>
      complete(response.marshal())
    case Success(response: TopicNotFoundException)    =>
      complete(response.marshal())
    case Failure(error: HyperQueueResponseFailure)    =>
      complete(error.marshal())
    case Failure(error: TopicNotFoundException)       =>
      complete(error.marshal())
    case Success(e: Exception)                        =>
      complete(HyperQueueException(e).marshal())
    case Failure(e: Exception)                        =>
      complete(HyperQueueException(e).marshal())
    case unknown: Any                                 =>
      complete(HttpResponse(InternalServerError).withEntity(unknown.toString))
  }

}