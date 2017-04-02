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

import akka.actor.Actor
import com.grmontpetit.model.exceptions.HyperQueueException
import spray.http.HttpResponse
import spray.routing.{HttpService, RejectionHandler, Route}

class HyperQueueServiceActor extends Actor with HyperQueueService {
  def actorRefFactory = context
  def receive = runRoute(hqRoutes)
}

trait HyperQueueService  extends HttpService {

  implicit val arf = actorRefFactory

  // Map Spray rejections to our standard response for rejections.
  def jsonifyError(response: HttpResponse): HttpResponse = {
    HyperQueueException(response.status, response.entity.asString, "No additional info").marshal()
  }
  implicit val rejectionHandler: RejectionHandler = RejectionHandler {
    case rejections => mapHttpResponse(jsonifyError) {
      RejectionHandler.Default(rejections)
    }
  }

  val services: Seq[HyperQueueApi] = Seq(
    new TopicService
  )

  /**
    * Concatenate all routes defined in every Services.
    * @return A Route containing all the Path served by the HQ Service.
    */
  def hqRoutes: Route = {
    services.map(_.routes).reduce(_ ~ _)
  }

}
