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

package com.grmontpetit.model

import com.grmontpetit.model.data.{ConnectionClosed, Error, Event, ModelObject, Topic}
import spray.httpx.SprayJsonSupport
import spray.json._

object JsonModelObject extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val eventInfo = jsonFormat2(Event)
  implicit val topicInfo = jsonFormat1(Topic)
  implicit val errorInfo = jsonFormat1(Error)
  implicit val connectionClosedInfo = jsonFormat1(ConnectionClosed)

  implicit object ModelObjectJsonFormat extends RootJsonFormat[ModelObject] {
    def write(obj: ModelObject) = obj match {
      case o: Event             => o.toJson
      case o: Topic             => o.toJson
      case o: Error             => o.toJson
      case o: ConnectionClosed  => o.toJson
      case _                    => throw new DeserializationException("This object doesn't have a json representation.")
    }
    def read(value: JsValue) = throw new DeserializationException("Not Implemented")
  }
}
