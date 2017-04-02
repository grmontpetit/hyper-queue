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

package com.grmontpetit.core

import akka.actor.{ActorRef, ActorSelection, ActorSystem, Props}
import akka.routing.RoundRobinPool
import com.grmontpetit.apis.HyperQueueServiceActor
import com.grmontpetit.managers.{EventManager, QueueManager}
import com.typesafe.config.ConfigFactory

object ActorCatalogue {

  implicit val system = ActorSystem("hyper-queue")
  val config = ConfigFactory.load()
  val concurrentConnections = config.getInt("service.concurrentConnections")

  val actorList = Set[Class[_]] (
    classOf[QueueManager]
  )

  /**
    * Initialize all the actors
    * @return The BrokerServiceActor which is the main actor responsible for managing all APIs.
    */
  def apply(): ActorRef = {
    val brokerService = classOf[HyperQueueServiceActor]
    val broker = system.actorOf(Props(brokerService), brokerService.getName)
    val eventManager = classOf[EventManager]
    system.actorOf(Props(eventManager).withRouter(RoundRobinPool(nrOfInstances = concurrentConnections)),
      eventManager.getName)
    actorList.foreach(ref => {
      system.actorOf(Props(ref), ref.getName)
    })
    broker
  }

  /**
    * select any actor of the micro service based on its type.
    * If this actor cannot be selected, it instantiate a new one with no default name.
    *
    * @param ref classOf[Actor]
    * @return an actorRef on selected actor
    */
  def getActor[A](ref: Class[A]): ActorSelection = {
    system.actorSelection(s"/user/${ref.getName}")
  }

}