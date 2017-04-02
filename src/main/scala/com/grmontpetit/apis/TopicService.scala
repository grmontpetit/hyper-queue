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

import akka.pattern.ask
import akka.actor.ActorRefFactory
import akka.util.Timeout
import com.grmontpetit.core.ActorCatalogue
import com.grmontpetit.managers.TopicManager
import com.grmontpetit.model.messages.{Consume, GetTopics, Produce}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

class TopicService(implicit context: ActorRefFactory) extends HyperQueueApi {

  def actorRefFactory = context
  import context.dispatcher

  def routes = getTopics ~ consumeTopic ~ produceTopic

  lazy val topicManager = ActorCatalogue.getActor(classOf[TopicManager])
  val configuration = ConfigFactory.load()
  implicit val timeout = Timeout(configuration.getInt("service.timeout").seconds)

  def getTopics = path("") {
    get {
      onComplete(topicManager ? GetTopics()) {
        futureHandler
      }
    }
  }

  def produceTopic = path(Segment) { topic =>
    post {
      onComplete(topicManager ? Produce(topic, "This is an event")) {
        futureHandler
      }
    }
  }

  def consumeTopic = path(Segment) { topic =>
    get {
      onComplete(topicManager ? Consume(topic)) {
        futureHandler
      }
    }
  }
}
