package org.example.hellostream.api

import akka.NotUsed
import akka.stream.scaladsl.Source
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceCall}
import org.example.hello.api.GreetingMessage

/**
  * The hello stream interface.
  *
  * This describes everything that Lagom needs to know about how to serve and
  * consume the HelloStream service.
  */
trait HelloStreamService extends Service {

  def establishFeed(assetId: String): ServiceCall[NotUsed, Source[GreetingMessage, NotUsed]]

  override final def descriptor: Descriptor = {
    import Service._

    named("hello-stream")
      .withCalls(
        pathCall("/stream/:assetId", establishFeed _)
      ).withAutoAcl(true)
  }
}

