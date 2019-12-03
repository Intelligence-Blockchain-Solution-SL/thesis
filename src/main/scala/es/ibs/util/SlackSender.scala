package es.ibs.util

import java.util.concurrent.TimeUnit.MILLISECONDS
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.pattern.after
import akka.stream.ActorMaterializer
import play.api.libs.json.Json
import es.ibs.util.SlackClient.Message

trait SlackSender extends Logging {

  protected val slackChannel: String
  protected val slackSender: String
  protected val slackIcon: Option[String]

  implicit val system: ActorSystem
  implicit val mat: ActorMaterializer
  implicit val ec: ExecutionContext

  private val maxMsgLength = 39000 //According doc limit for message is 40000, but we book some for internal error message
  private val batchDelay = new FiniteDuration(4000, MILLISECONDS)

  def send2slack(message: String): Future[Any] =
    send2slack(slackChannel,slackIcon, slackSender, message)

  def send2slack(channel: String, icon: Option[String], sender: String, message: String): Future[Any] = {
    if (channel == null || channel == "") return Future.unit
    val msg = SlackClient.Message(
      mrkdwn = false,
      username = Some(sender),
      icon_emoji = icon,
      text = Some(message)
    )
    post(channel, msg).recover {
      case err: Exception =>
        LOG_E(err,"Unable to send notification to slack")
    }
  }

  def send2slack(messages: Seq[String]): Future[Any] =
    send2slack(slackChannel,slackIcon, slackSender, messages)

  def send2slack(channel: String, icon: Option[String], sender: String, messages: Seq[String]): Future[Any] = {
    if (messages.isEmpty) return Future.unit
    if (channel == null || channel == "") return Future.unit

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
    send2slack(channel,icon,sender,bigMessage.mkString).flatMap { _ =>
      after(batchDelay, system.scheduler)(send2slack(channel,icon,sender,tail)) }
  }

  def post(webhook: String, message: Message): Future[(Int, String)] = {
    val hookReq = HttpRequest().withUri(webhook).withMethod(HttpMethods.POST)
    Http().singleRequest(hookReq.withEntity(HttpEntity(ContentTypes.`application/json`, Json.toJson(message).toString))) flatMap { resp =>
      Unmarshal(resp.entity).to[String] map { str => resp.status.intValue() -> str}
    }
  }
}

