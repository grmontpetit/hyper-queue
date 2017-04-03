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
import com.grmontpetit.model.exceptions.ConnectionClosedException
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Await
import scala.concurrent.duration._

class EventManager extends Actor with LazyLogging {

  val config = ConfigFactory.load()
  implicit val timeout = Timeout(config.getInt("service.timeout").seconds)
  lazy val queueManager = ActorCatalogue.getActor(classOf[QueueManager])

  def receive: Receive = {
    case Consume(topic: String, id: Int)      => sender ! blockingConsume(topic, id)
    case GetTopics()                          => sender ! getTopics()
    case Produce(topic: String, event: Event) => sender ! produce(topic, event)
    case GetTopicEvents(topic: String)        => sender ! getTopicEvents(topic)
    case GenerateId(topic: String)            => sender ! generateId(topic)
    case _                                    => Unit
  }

  /**
    * Generates an id and returns it to the consumer
    * if it's the first connection.
    * @param topic The topic to generate the id with.
    * @return The id wrapped inside a [[ConsumerAccepted]] object.
    */
  private def generateId(topic: String): Status = {
    val id = askQueueInstance.generateId()
    Status.Success(ConsumerAccepted(id))
  }

  /**
    * Consume an event from the queue from a given topic in a
    * blocking manner.
    * @param topic The topic to consume from.
    * @return The [[Event]] wrapped inside an [[ItemConsumed]] object.
    */
  private def blockingConsume(topic: String, id: Int): Status = {
    val futureEvent = askQueueInstance.pop(topic, id)
    try {
      val result = Await.result(futureEvent, 6.seconds)
      Status.Success(ItemConsumed(result))
    } catch {
      case e: ConnectionClosedException => Status.Failure(ItemTimeout(ConnectionClosed("Connection Closed")))
    }
  }

  /**
    * Adds an event into a topic. If the topic doesn't exits,
    * the [[HyperQueue]] will create it.
    * @param topic The topic to add to / create
    * @param event The event to add.
    * @return The [[Event]] wrapped inside an [[ItemProduced]] object.
    */
  private def produce(topic: String, event: Event): Status = {
    logger.info(s"producing event $event for topic $topic")
    askQueueInstance.push(topic, event)
    Status.Success(ItemProduced(event))
  }

  /**
    * Retrieves all topics from the [[HyperQueue]]
    * @return The the topics as an instance of a [[List]] wrapped
    *         inside an [[ItemsInfo]] object.
    */
  private def getTopics(): Status = {
    val topics = askQueueInstance.getTopicList
    logger.info(s"returning current topics: $topics")
    Status.Success(ItemsInfo(topics.toList.map(Topic)))
  }

  /**
    * Retrieves all event of a given topic from the [[HyperQueue]].
    * @param topic The topic to retrieve from.
    * @return A [[List]] of [[Event]] wrapped inside an [[ItemsInfo]] object.
    */
  private def getTopicEvents(topic: String): Status = {
    logger.info(s"Retrieving all events for topic $topic")
    val events = askQueueInstance.getTopicEvents(topic)
    Status.Success(ItemsInfo(events.toList.map(event => Event(event.id, event.value))))
  }

  /**
    * Ask the [[QueueManager]] to have access to the [[HyperQueue]].
    * @return The [[HyperQueue]] instance.
    */
  private def askQueueInstance: HyperQueue = Await.result(queueManager ? GetQueueInstance, 2.seconds).asInstanceOf[HyperQueue]

}
