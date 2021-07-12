package org.example.hellostream.impl

import akka.Done
import akka.stream.scaladsl.Flow
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.pubsub.{PubSubRegistry, TopicId}
import org.example.hellostream.api.HelloStreamService
import org.example.hello.api.{GreetingMessage, HelloService}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.Future

/**
  * Implementation of the HelloStreamService.
  */
class HelloStreamServiceImpl(helloService: HelloService, pubSub: PubSubRegistry) extends HelloStreamService {
  private final val log: Logger =
  LoggerFactory.getLogger(classOf[HelloStreamServiceImpl])

  helloService.greetingsTopic.subscribe.atLeastOnce(Flow.fromFunction(greeting => {
    println(s"RECEIVED EVENT IN TOPIC ${greeting.name} ${greeting.message}")
    val topic = pubSub.refFor(TopicId[GreetingMessage](greeting.name))
    topic.publish(GreetingMessage(greeting.message))
    Done
  }))

  def establishFeed(assetId: String) = ServiceCall { _ =>
    log.info(s"Request to establish feed received for asset id: $assetId")
    val topic = pubSub.refFor(TopicId[GreetingMessage](assetId))
    Future.successful(topic.subscriber)
  }
}
