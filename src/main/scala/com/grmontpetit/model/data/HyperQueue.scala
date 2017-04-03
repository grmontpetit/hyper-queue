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

package com.grmontpetit.model.data

import java.util
import java.util.Collections
import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue, TimeUnit}

import com.grmontpetit.model.exceptions.{ConnectionClosedException, TopicNotFoundException}

import scala.concurrent.{Future, Promise}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

// Stateful object
class HyperQueue() {

  // When adding new List, they *must* be synchronizedList !
  private val queue = new ConcurrentHashMap[String, util.List[Event]]
  // store the nb of consumers registered to the broker
  // we don't track the consumers's id, just the nb. consumers connected.
  private var consumerQty = -1 // start at -1, on first connect it will be incremented to 0

  /**
    * Adds a new event to the queue.
    * @param topic
    * @param event
    * @return
    */
  def push(topic: String, event: Event) = {
    if (this.queue.containsKey(topic)) {
      this.queue.get(topic).add(event)
    } else {
      val list = new util.ArrayList[Event]()
      list.add(event)
      val queuedEvent = Collections.synchronizedList(list)
      // The list *must* be a synchronizedList
      this.queue.put(topic, queuedEvent)
    }
  }

  /**
    * Removes an event from a topic from a given id.
    * The id is the id of the consumer.
    * @param topic The topic to consume from
    * @param id The id to consume
    * @return An [[Event]] wrapped into a [[Future]]
    */
  def pop(topic: String, id: Int): Future[Event] = {
    val p = Promise[Event]()
    Future {
      if (this.queue.containsKey(topic)) {
        val event = retry(5000, this.queue.get(topic), id)
        if (event != null) {
          this.queue.get(topic).remove(id)
          p.success(event)
        } else {
          p.failure(new ConnectionClosedException)
        }
      } else {
        p.failure(new TopicNotFoundException)
      }
    }
    p.future
  }

  /**
    * Retries to consume an event at a position X which
    * is represented by the id parameter.
    * @param timeoutMilis The the retry time in ms.
    * @param list The [[util.List]] of [[Event]]
    * @param id The id (position) to read the queue from.
    * @return The [[Event]] if it's found.
    */
  private def retry(timeoutMilis: Long, list: util.List[Event], id: Int): Event = {
    val t0 = System.currentTimeMillis()
    if ((list.isEmpty || list.get(id) == null) && timeoutMilis > 0) {
      Thread.sleep(1000) // don't like this too much, too tired to use nano
      val t1 = System.currentTimeMillis()
      val elapsedTime = t1 - t0
      retry(timeoutMilis - elapsedTime, list, id)
    } else {
      if (list.isEmpty) {
        null
      } else {
        list.get(id)
      }
    }
  }

  /**
    * Retrieve the nb. of registered consumers.
    * @return The nb. of registered consumers as [Int]
    */
  def getRegisteredConsumersSize: Int = this.consumerQty

  /**
    * Generates an index to fetch data from.
    * @return The index to dequeue from.
    */
  def generateId(): Int = {
    this.consumerQty = consumerQty + 1
    consumerQty
  }

  /**
    * Returns the size of the Queue for a given topic.
    * @param topic The even't topic.
    * @return The Size of the queue as an [[Int]]
    */
  def size(topic: String): Int = this.queue.get(topic).size

  /**
    * returns the size of the event list from a given queue.
    * @param topic The even't topic.
    * @return The Size of the queue as an [[Int]]
    */
  def queueSize(topic: String): Int = this.queue.get(topic).size

  /**
    * Retrieves the list of available topics from the queue.
    * @return The list of existing topics.
    */
  def getTopicList: Iterable[String] = queue.asScala.keys

  /**
    * Retrieves all the events for a given topic.
    * @param topic The topic as a [[String]]
    * @return The [[Iterable]] list of [[Event]]
    */
  def getTopicEvents(topic: String): Iterable[Event] = {
    if (this.queue.containsKey(topic)) {
      queue.get(topic).asScala
    } else {
      Iterable.empty
    }
  }
}