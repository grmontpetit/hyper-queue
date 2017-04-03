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
import com.grmontpetit.managers.{EventManager, QueueManager}
import com.grmontpetit.model.data.{Event, HyperQueue}
import com.grmontpetit.model.messages._
import com.grmontpetit.model.JsonModelObject._
import com.grmontpetit.model.exceptions.HyperQueueException
import com.typesafe.config.ConfigFactory
import spray.http.StatusCodes
import spray.routing.Route

import scala.concurrent.Await
import scala.concurrent.duration._

class EventService(implicit context: ActorRefFactory) extends HyperQueueApi {

  def actorRefFactory = context
  import context.dispatcher

  /**
    * Aggregates all the TopicService Spray Routes.
    * @return An aggregated route which is
    */
  def routes = consumeEvent ~ produceEvent ~ getTopicEvents

  lazy val eventManager = ActorCatalogue.getActor(classOf[EventManager])
  val configuration = ConfigFactory.load()
  implicit val timeout = Timeout(configuration.getInt("service.timeout").seconds)
  lazy val queueManager = ActorCatalogue.getActor(classOf[QueueManager])

  /**
    * Default route to produce a topic, using an http POST directive.
    * @return A Spray Route to produce a topic.
    */
  def produceEvent: Route = path(Segment) { topic =>
    post {
      entity(as[Event]) { event =>
        onComplete(eventManager ? Produce(topic, event)) {
          futureHandler
        }
      }
    }
  }

  /**
    * Default route to consume a topic, using an http GET directive.
    * If the id key is present in the header, it is extracted and
    * passed to the EventManager as an Int.
    * @return A Spray Route to consume an event within a topic.
    */
  def consumeEvent: Route = path(Segment) { topic =>
    get {
      // there is a more elegant way of doing this, going to keep it simple for now
      optionalHeaderValueByName("id") { id =>
        optionalHeaderValueByName("OFFSET") { offset =>
          if (offset.isDefined) {
            val newId = offset.get match {
              case "-1" => 0
              case "1"  => askQueueInstance.queueSize(topic)
              case _    => throw HyperQueueException(StatusCodes.BadRequest, "Invalid OFFSET", "Invalid OFFSET")
            }
            onComplete(eventManager ? Consume(topic, newId.asInstanceOf[Int])) {
              futureHandler
            }
          } else if (id.isDefined) {
            onComplete(eventManager ? Consume(topic, id.get.toInt)) {
              futureHandler
            }
          } else {
            onComplete(eventManager ? GenerateId(topic)) {
              futureHandler
            }
          }
        }
      }
    }
  }

  /**
    * Default route to view all events for a given topic.
    * @return A Spray Route to view all events for a given topic.
    */
  def getTopicEvents: Route = path(Segment / "events") { topic =>
    get {
      onComplete(eventManager ? GetTopicEvents(topic)) {
        futureHandler
      }
    }
  }

  // Duplicated function
  /**
    * Ask the [[QueueManager]] to have access to the [[HyperQueue]].
    * @return The [[HyperQueue]] instance.
    */
  private def askQueueInstance: HyperQueue = Await.result(queueManager ? GetQueueInstance, 2.seconds).asInstanceOf[HyperQueue]
}
