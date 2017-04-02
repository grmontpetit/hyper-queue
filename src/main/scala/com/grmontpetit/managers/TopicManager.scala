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

import akka.actor.{Actor, Status}
import akka.actor.Status.Status
import com.grmontpetit.model.messages.{Consume, GetTopics, Produce}
import com.typesafe.scalalogging.LazyLogging

class TopicManager extends Actor with LazyLogging {

  def receive: Receive = {
    case Consume(topic: String)               => sender ! consume(topic)
    case GetTopics()                          => sender ! getTopics()
    case Produce(topic: String, data: String) => sender ! produce(topic, data)
    case _                                    => Unit
  }

  def consume(topic: String): Status = {
    logger.info(s"consuming topic $topic")
    Status.Success(s"consuming topic $topic")
  }

  def produce(topic: String, data: String): Status = {
    logger.info(s"producing topic $topic")
    Status.Success(s"consuming topic $topic")
  }

  def getTopics(): Status = {
    logger.info(s"returning current topics: topic1, topic2, topic3")
    Status.Success(s"returning current topics: topic1, topic2, topic3")
  }

}
