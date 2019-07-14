package es.ibs.util

import java.util.concurrent.TimeUnit.MILLISECONDS
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.pattern.after
import akka.stream.ActorMaterializer

trait SlackSender extends Logging {

  protected val slackChannel: String
  protected val slackSender: String
  protected val slackIcon: Option[String]

  implicit val system: ActorSystem
  implicit val mat: ActorMaterializer
  implicit val ec: ExecutionContext

  private val maxMsgLength = 39000 //According doc limit for message is 40000, but we book some for internal error message
  private val batchDelay = new FiniteDuration(4000, MILLISECONDS)

  private var slk: SlackClient = null

  def send2slack (message: String): Future[Any] = {
    if (slackChannel == null || slackChannel == "") return Future.unit
    if (slk == null) slk = new SlackClient(slackChannel)
    val msg = SlackClient.Message(
      mrkdwn = false,
      username = Some(slackSender),
      icon_emoji = slackIcon,
      text = Some(message)
    )
    slk.post(msg).recover {
      case err: Exception =>
        LOG_E(err,"Unable to send notification to slack")
    }
  }

  def send2slack (messages: Seq[String]): Future[Any] = {
    if (messages.isEmpty) return Future.unit
    if (slackChannel == null || slackChannel == "") return Future.unit
    if (slk == null) slk = new SlackClient(slackChannel)

    val bigMessage = new StringBuilder

    def appendMsgs(mgs: Seq[String]): Seq[String] = {
      if (mgs.isEmpty) mgs
      else if (mgs.head.length > maxMsgLength) {
        LOG_E(s"Message length ${mgs.head.length} exceeds limit $maxMsgLength. Following messages omitted")
        bigMessage ++= s":exclamation: Some messages omitted due the message length ${mgs.head.length} exceeds limit $maxMsgLength"
        Seq[String]()
      }
      else if (bigMessage.size + mgs.head.length > maxMsgLength) mgs
      else {
        bigMessage ++= mgs.head
        bigMessage += '\n'
        appendMsgs(mgs.tail)
      }
    }

    val tail = appendMsgs(messages)
    LOG_D(s"Sender processed ${messages.size - tail.size} messages in a batch. Remaining messages: ${tail.size}")
    send2slack(bigMessage.mkString).flatMap { _ => after(batchDelay, system.scheduler)(send2slack(tail)) }
  }
}

