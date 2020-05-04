package es.ibs.util

import scala.concurrent.ExecutionContextExecutor
import akka.actor.{Actor, ActorSystem}
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

trait ActorBase extends Actor with Logging {
  import scala.concurrent.duration._

  import akka.actor.ActorKilledException
  import akka.util.Timeout

  val name: String = '[' + context.self.path.name + ']' // actor name
  var info: String = "" // additional actor info

  implicit val system: ActorSystem = context.system
  implicit val ec: ExecutionContextExecutor = context.dispatcher
  implicit val timeout: Timeout = Timeout(10.seconds)

  override val logger: Logger =
    Logger(LoggerFactory.getLogger(s"${getClass.getPackage.getName}.${context.self.path.name}"))

  // helpful routines
  @scala.inline final def NOW: Int = es.ibs.util.NOW

  // additional actions on start and stop
  override def preStart(): Unit = {
    super.preStart()
    LOG_I("started" + (if (info.nonEmpty) s" '$info'" else ""))
  }

  override def postStop(): Unit = {
    LOG_I("stopped")
    super.postStop()
  }

  override def unhandled(message : scala.Any): Unit = {
    val from = sender()
    if (!(message.isInstanceOf[ActorKilledException] && from == self))
      LOG_W(s"received something strange '${message.toString}' from [${from.toString()}]")
  }

}