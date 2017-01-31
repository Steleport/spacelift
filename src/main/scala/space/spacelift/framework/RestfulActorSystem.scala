package space.spacelift.framework

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

import akka.actor._
import space.spacelift.mq.proxy.ProxiedActorSystem
import javax.inject._

import akka.NotUsed
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.testkit.TestActorRef
import akka.util.Timeout

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._

@Singleton
class RestfulActorSystem @Inject() (proxiedActorSystem: ProxiedActorSystem) {
  import proxiedActorSystem._

  private val actorMap: scala.collection.concurrent.Map[String, (ActorRef, Actor.Receive)] = new ConcurrentHashMap[String, (ActorRef, Actor.Receive)]().asScala
  private val classMap: scala.collection.concurrent.Map[String, List[String]] = new ConcurrentHashMap[String, List[String]]().asScala

  def loadClassList(root: File, file: File): List[String] = {
    if (!file.isDirectory) {
      if (file.getName.toLowerCase().endsWith(".jar")) {
        try {
          new JarFile(file).entries().asScala.map(_.getName).filter(_.lastIndexOf(".class") > 0).map(n => n.substring(0, n.lastIndexOf(".class")).replace("/", ".")).toList
        } catch {
          case _ => List()
        }
      } else {
        if (file.getName.toLowerCase().endsWith(".class")) {
          try {
            List(file.getAbsolutePath.substring(file.getAbsolutePath.lastIndexOf(root.getAbsolutePath) + root.getAbsolutePath.length + 1, file.getAbsolutePath.lastIndexOf(".class")).replace("/", "."))
          } catch {
            case _ => List()
          }
        } else List()
      }
    } else {
      file.listFiles().toList.flatMap(c => loadClassList(root, c))
    }
  }

  def startServer(implicit system: ActorSystem) = {
    val list = this.getClass.getClassLoader.getResources("").asScala.flatMap(p => loadClassList(new File(p.getPath), new File(p.getPath))).toList
    println(list)

    val routes: Flow[HttpRequest, HttpResponse, NotUsed] = Flow[HttpRequest].map { req =>
      val key = req.uri.path.toString.split("/").drop(1).head
      if (actorMap.contains(key)) {
        if (!classMap.contains(key)) {
          classMap.put(key, list.filter(c => try {
            actorMap(key)._2.isDefinedAt(this.getClass.getClassLoader.loadClass(c).newInstance())
          } catch {
            case _ => false
          }))
        }

        implicit val timeout: Timeout = 30 seconds
        val msgKey = req.uri.path.toString.split("/").drop(2).head
        println(msgKey)
        val msg = this.getClass.getClassLoader.loadClass(classMap(key).filter(_.split("\\.").last.equals(msgKey)).head).newInstance()

        HttpResponse(StatusCodes.OK, entity = HttpEntity(Await.result(actorMap(key)._1 ? msg, 30 seconds).asInstanceOf[String]))
      } else {
        HttpResponse(StatusCodes.OK)
      }
    }

    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val bindingFuture = Http().bindAndHandle(routes, "localhost", 8080)
  }

  implicit class RestfulActorOf(system: ActorSystem) {
    def restfulActorOf(props: Props, name: String): ActorRef = {
      val server = system.rpcServerActorOf(props, name)
      val client = system.rpcClientActorOf(props, name)

      actorMap.put(name, (client, TestActorRef(props)(system).underlyingActor.receive))

      server
    }
  }
}
