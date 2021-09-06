package org.example.hello.impl

import akka.cluster.sharding.typed.scaladsl.Entity
import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.cluster.ClusterComponents
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraPersistenceComponents
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.server._
import com.softwaremill.macwire._
import org.example.hello.api.HelloService
import org.example.hello.impl.readside.{GreetingsProcessor, GreetingsRepository}
import play.api.libs.ws.ahc.AhcWSComponents

class HelloLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new HelloApplication(context) with AkkaDiscoveryComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new HelloApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[HelloService])
}

abstract class HelloApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with CassandraPersistenceComponents
    with LagomKafkaComponents
    with ClusterComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[HelloService](wire[HelloServiceImpl])

  lazy val greetingsRepository = wire[GreetingsRepository]
//  readSide.register[HelloEvent](wire[GreetingsProcessor])
  readSide.register[HelloEvent](new GreetingsProcessor(cassandraSession, cassandraReadSide))

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = HelloSerializerRegistry

  // Initialize the sharding of the Aggregate. The following starts the aggregate Behavior under
  // a given sharding entity typeKey.
  clusterSharding.init(
    Entity(HelloState.typeKey)(
      entityContext => HelloBehavior.create(entityContext)
    )
  )

}
