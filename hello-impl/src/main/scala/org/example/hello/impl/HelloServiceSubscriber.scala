package org.example.hello.impl

import akka.Done
import akka.stream.scaladsl.Flow
import org.example.hello.api.HelloService

class HelloServiceSubscriber(helloService: HelloService) {
  helloService.greetingsTopic.subscribe.atLeastOnce(Flow.fromFunction(greeting => {
    println(s"RECEIVED EVENT IN TOPIC ${greeting.name} ${greeting.message}")
    Done
  }))

}
