package org.example.hellostream.impl

import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaClientComponents
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.playjson.{EmptyJsonSerializerRegistry, JsonSerializerRegistry}
import com.lightbend.lagom.scaladsl.pubsub.PubSubComponents
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import org.example.hello.api.HelloService
import org.example.hellostream.api.HelloStreamService
import play.api.libs.ws.ahc.AhcWSComponents

class HelloStreamLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new HelloStreamApplication(context) with AkkaDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HelloStreamApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HelloStreamService])
}

abstract class HelloStreamApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with ClusterComponents
    with LagomKafkaClientComponents
    with PubSubComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[HelloStreamService](wire[HelloStreamServiceImpl])

  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = EmptyJsonSerializerRegistry

  // Bind the HelloService client
  lazy val helloService: HelloService = serviceClient.implement[HelloService]
}

