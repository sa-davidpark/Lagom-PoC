package org.example.hello.impl.readside

import com.datastax.driver.core.SimpleStatement
import com.lightbend.lagom.scaladsl.persistence.cassandra.CassandraSession
import org.example.hello.api.Greeting

import scala.concurrent.{ExecutionContext, Future}

class GreetingsRepository(
                           session: CassandraSession
                         )(
                           implicit ec: ExecutionContext
                         ) {

  def getAll(): Future[Seq[Greeting]] = {
    session.selectAll(new SimpleStatement("SELECT name, message FROM greeting"))
      .map { rows =>
        println(s"ROWSSS-------")
        rows.map(row => {
          println(s"ROW: $row")
          Greeting(row.getString("name"), row.getString("message"))
        })
      }
    }
}