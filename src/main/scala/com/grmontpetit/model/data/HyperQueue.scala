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

import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue, TimeUnit}

import com.grmontpetit.model.exceptions.TopicNotFoundException

import scala.concurrent.{Future, Promise}
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

// Stateful object
class HyperQueue() {

  // key = topic, value = events
  private val queue = new ConcurrentHashMap[String, LinkedBlockingQueue[Event]](30)

  /**
    * Adds an element to a topic queue. If the topic doesn't exist,
    * it is added to the topic list.
    * @param topic Under which topic the event must be added.
    * @param event The [[Event]] to add to the topic.
    */
  def push(topic: String, event: Event) = {
    if (this.queue.containsKey(topic)) {
      this.queue.get(topic).put(event)
    } else {
      val queuedEvent = new LinkedBlockingQueue[Event]()
      queuedEvent.put(event)
      this.queue.put(topic, queuedEvent)
    }
  }

  /**
    * Try to pop the queue for a given topic. Waits
    * 5 seconds, fails the future if no element are
    * added while waiting.
    * @param topic The topic to add an event to.
    * @return An [[Event]] wrapped into a [[scala.concurrent.Future]]
    */
  def pop2(topic: String): Future[Event] = {
    if (this.queue.containsKey(topic)) {
      Future.successful(queue.get(topic).poll(5, TimeUnit.SECONDS))
    } else {
      Future.failed(new TopicNotFoundException)
    }
  }

  /**
    * Pops the queue in a polling fashion for 5 seconds.
    * @param topic The topic to pop.
    * @return The [[Event]] wrapped in a [[Future]]
    */
  def pop(topic: String): Future[Event] = {
    val p = Promise[Event]()
    Future {
      if (this.queue.containsKey(topic)) {
        p.success(queue.get(topic).poll(5, TimeUnit.SECONDS))
      } else {
        p.failure(new TopicNotFoundException)
      }
    }
    p.future
  }

  /**
    * Returns the size of the Queue for a given topic.
    * @param topic The even't topic.
    * @return The Size of the queue as an [[Int]]
    */
  def size(topic: String): Int = this.queue.get(topic).size

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
    // gotta use the java way here
    if (this.queue.containsKey(topic)) {
      queue.get(topic).asScala
    } else {
      Iterable.empty
    }
  }

}