package org.example.hello.impl

import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.util.Timeout
import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.transport.BadRequest
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import org.example.hello.api
import org.example.hello.api.HelloService
import org.example.hello.impl.readside.GreetingsRepository

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Implementation of the HelloService.
  */
class HelloServiceImpl(
  greetingsRepository: GreetingsRepository,
  clusterSharding: ClusterSharding,
  persistentEntityRegistry: PersistentEntityRegistry
)(implicit ec: ExecutionContext)
  extends HelloService {

  /**
    * Looks up the entity for the given ID.
    */
  private def entityRef(id: String): EntityRef[HelloCommand] =
    clusterSharding.entityRefFor(HelloState.typeKey, id)

  implicit val timeout = Timeout(5.seconds)

  override def hello(id: String): ServiceCall[NotUsed, String] = ServiceCall {
    _ =>
      // Look up the sharded entity (aka the aggregate instance) for the given ID.
      val ref = entityRef(id)

      // Ask the aggregate instance the Hello command.
      ref
        .ask[Greeting](replyTo => Hello(id, replyTo))
        .map(greeting => greeting.message)
  }

  override def useGreeting(id: String) = ServiceCall { request =>
    // Look up the sharded entity (aka the aggregate instance) for the given ID.
    val ref = entityRef(id)

    // Tell the aggregate to use the greeting message specified.
    ref
      .ask[Confirmation](
        replyTo => UseGreetingMessage(request.message, replyTo)
      )
      .map {
        case Accepted => Done
        case _        => throw BadRequest("Can't upgrade the greeting message.")
      }
  }

  override def greetingsTopic(): Topic[api.GreetingMessageChanged] =
    TopicProducer.singleStreamWithOffset { fromOffset =>
      persistentEntityRegistry
        .eventStream(HelloEvent.Tag, fromOffset)
        .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(
    helloEvent: EventStreamElement[HelloEvent]
  ): api.GreetingMessageChanged = {
    helloEvent.event match {
      case GreetingMessageChanged(msg) =>
        api.GreetingMessageChanged(helloEvent.entityId, msg)
    }
  }

  override def allGreetings() = ServiceCall { request =>
    greetingsRepository.getAll()
  }
}
