package org.example.hello.impl.readside

import akka.Done
import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraReadSide, CassandraSession}
import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, EventStreamElement, ReadSideProcessor}
import org.example.hello.impl.{GreetingMessageChanged, HelloEvent}

import scala.concurrent.{ExecutionContext, Future, Promise}

class GreetingsProcessor(session: CassandraSession, readSide: CassandraReadSide)(implicit ec: ExecutionContext)
  extends ReadSideProcessor[HelloEvent] {

  private val createTableCql =
    "CREATE TABLE IF NOT EXISTS greeting (" +
      "\n    name text PRIMARY KEY," +
      "\n    message text" +
      "\n)"

  // This is a convenience for creating the read-side table in development mode.
  val buildTables: Future[Done] =
    session.executeCreateTable(createTableCql);

  private val writeGreetingPromise = Promise[PreparedStatement] // initialized in prepare

  private def writeGreeting: Future[PreparedStatement] = writeGreetingPromise.future

  private def prepareWriteGreeting(): Future[Done] = {
    val f = session.prepare("INSERT INTO greeting (name, message) VALUES (?, ?)")
    writeGreetingPromise.completeWith(f)
    f.map(_ => Done)
  }

  private def processGreetingMessageChanged(
                                             eventElement: EventStreamElement[GreetingMessageChanged]
                                           ): Future[List[BoundStatement]] = {
    println("Received event and adding to db")
    writeGreeting.map { ps =>
      val bindWriteGreeting = ps.bind()
      bindWriteGreeting.setString("name", eventElement.entityId)
      bindWriteGreeting.setString("message", eventElement.event.message)
      List(bindWriteGreeting)
    }
  }

  override def buildHandler() =
    readSide
      .builder[HelloEvent]("GreetingsReadSideOffset1")
//      .setGlobalPrepare(() => buildTables)
      .setPrepare(_ => prepareWriteGreeting())
      .setEventHandler[GreetingMessageChanged](processGreetingMessageChanged)
      .build()

  override def aggregateTags: Set[AggregateEventTag[HelloEvent]] = Set(HelloEvent.Tag)
}
