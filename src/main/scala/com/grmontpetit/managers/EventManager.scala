/*
 * Copyright 2017 Gabriel Robitaille-Montpetit
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

package com.grmontpetit.managers

import akka.pattern.ask
import akka.actor.Status.Status
import akka.actor.{Actor, Status}
import akka.util.Timeout
import com.grmontpetit.apis.HyperQueueApi._
import com.grmontpetit.core.ActorCatalogue
import com.grmontpetit.model.data._
import com.grmontpetit.model.messages._
import com.grmontpetit.model.JsonModelObject._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


class EventManager extends Actor with LazyLogging {

  val config = ConfigFactory.load()
  implicit val timeout = Timeout(config.getInt("service.timeout").seconds)
  val queueManager = ActorCatalogue.getActor(classOf[QueueManager])

  def receive: Receive = {
    case Consume(topic: String)               => sender ! blockingConsume(topic)
    case GetTopics()                          => sender ! getTopics()
    case Produce(topic: String, event: Event) => sender ! produce(topic, event)
    case GetTopicEvents(topic: String)        => sender ! getTopicEvents(topic)
    case _                                    => Unit
  }

  def unblockingConsume(topic: String) = {

    val futureEvent = askQueueInstance.pop(topic)
    futureEvent.onComplete {
      case Success(event) =>
        logger.info(s"consuming an event in topic $topic, event is: $event")
        Status.Success(ItemInfo(event))
      case Failure(error) =>
        logger.info(s"There was an error retrieving the topic $topic")
        Status.Failure(ItemError(Error(error.getMessage)))
    }
  }

  def blockingConsume(topic: String): Status = {
    val futureEvent = askQueueInstance.pop(topic)
    val result = Await.result(futureEvent, 6.seconds)
    if (result == null) {
      Status.Failure(ItemTimeout(ConnectionClosed("Connection Closed")))
    } else {
      Status.Success(ItemConsumed(result))
    }
  }

  def produce(topic: String, event: Event): Status = {
    logger.info(s"producing event $event for topic $topic")
    askQueueInstance.push(topic, event)
    Status.Success(ItemProduced(event))
  }

  def getTopics(): Status = {
    val topics = askQueueInstance.getTopicList
    logger.info(s"returning current topics: $topics")
    Status.Success(ItemsInfo(topics.toList.map(Topic)))
  }

  def getTopicEvents(topic: String): Status = {
    logger.info(s"Retrieving all events for topic $topic")
    val events = askQueueInstance.getTopicEvents(topic)
    Status.Success(ItemsInfo(events.toList.map(event => Event(event.id, event.value))))
  }

  def askQueueInstance: HyperQueue = Await.result(queueManager ? GetQueueInstance, 2.seconds).asInstanceOf[HyperQueue]

}
