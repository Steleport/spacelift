import RestfulActorSystemTests.TestActor
import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import space.spacelift.framework.RestfulActorSystem
import space.spacelift.mq.proxy.ProxiedActorSystem
import space.spacelift.mq.proxy.impl.amqp.AmqpConnectionWrapper

import scala.concurrent.duration._

object RestfulActorSystemTests {
  case class Foo()

  class TestActor extends Actor {
    def receive: Actor.Receive = {
      case Foo() => sender ! "AAAA"
    }
  }
}

@RunWith(classOf[JUnitRunner])
class RestfulActorSystemTests extends TestKit(ActorSystem("TestSystem")) with WordSpecLike with Matchers with BeforeAndAfter with ImplicitSender {
  implicit val timeout: akka.util.Timeout = 5 seconds

  "RestfulActorSystem" should {
    "create and respond to requests" in {
      val pas = new ProxiedActorSystem(new AmqpConnectionWrapper(system.settings.config))
      val ras = new RestfulActorSystem(pas)

      import ras._
      import pas._

      system.rpcServerActorOf(Props[TestActor], "foo")
      system.restfulActorOf(Props[TestActor], "foo")

      ras.startServer

      Thread.sleep(10000000)
    }
  }
}
