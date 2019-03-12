package es.ibs.util

import play.api.libs.json.{Json, Writes}
import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer

class SlackClient(URL: String)(implicit as: ActorSystem, ec: ExecutionContext, mat: Materializer) {
  import SlackClient._

  private val req = HttpRequest().withUri(URL).withMethod(HttpMethods.POST)

  def post(message: Message): Future[(Int, String)] = {
    Http().singleRequest(req.withEntity(HttpEntity(ContentTypes.`application/json`, Json.toJson(message).toString()))) flatMap { resp =>
      Unmarshal(resp.entity).to[String] map { str => resp.status.intValue() -> str}
    }
  }

  def post(text: String, mrkdwn: Boolean = false, link_names: Boolean = false): Future[(Int, String)] =
    post(Message(text = Some(text), mrkdwn = mrkdwn, link_names = link_names))
}

// =====================================================================================================================
// https://api.slack.com/docs/message-formatting
// =====================================================================================================================

object SlackClient {

  // https://<...>.slack.com/apps/manage/custom-integrations

  // https://api.slack.com/custom-integrations/incoming-webhooks
  // https://api.slack.com/custom-integrations/legacy-tokens

  // https://api.slack.com/methods/chat.postMessage
  // https://api.slack.com/methods/files.upload

  val emoji = Map(
    "W" -> Some(":warning:"),
    "I" -> Some(":information_source:"),
    "D" -> Some(":cactus:"),
    "E" -> Some(":interrobang:")
  )

  // "good", "warning", "danger" are predefined colors
  val color_good = Some("good")
  val color_warning = Some("warning")
  val color_danger = Some("danger")
  val colors = Map(
    "red" -> Some("#FF0000"),
    "green" -> Some("#00FF00"),
    "blue" -> Some("#0000FF"),
    "cyan" -> Some("#00FFFF"),
    "magenta" -> Some("#FF00FF"),
    "yellow" -> Some("#FFFF00"),
    "black" -> Some("#000000")
  )

  // style for actions (buttons) attachment
  val style_primary = Some("primary")
  val style_danger = Some("danger")

  // https://api.slack.com/docs/message-formatting#linking_to_urls
  @inline def href(ref: String, alt: String): String = s"<$ref|$alt>"

  case class AttachmentField(
    title: Option[String] = None, // bold field title
    value: Option[String] = None, // field value
    short: Boolean = true // attempt to put fields in one line
  )

  case class AttachmentActionConfirm(
    title: String,
    text: String,
    ok_text: String = "Yes",
    dismiss_text: String = "No"
  ) { val opt = Some(this) }

  case class AttachmentAction(
    text: String,
    url: String,
    style: Option[String] = style_primary,
    confirm: Option[AttachmentActionConfirm] = None,
    `type`: String = "button"
  )

  // class fields order represents real attachment sections order
  // https://api.slack.com/docs/message-attachments
  case class Attachment(
    pretext: Option[String] = None, // put this text before attachment
    color: Option[String] = None, // color mark of attachment
    author_name: Option[String] = None, // gray author name before title
    author_link: Option[String] = None, // makes author clickable
    author_icon: Option[String] = None, // author pic
    title: Option[String] = None, // bold title of attachment
    title_link: Option[String] = None, // make title clickable
    text: Option[String] = None, // attachment text
    image_url: Option[String] = None, // image inside of attachment
    thumb_url: Option[String] = None, // image on right side of attachment text
    fields: Seq[AttachmentField] = Seq.empty,
    actions: Seq[AttachmentAction] = Seq.empty, // buttons
    footer: Option[String] = None, // footer text
    footer_icon: Option[String] = None, // footer icon (before text),
    ts: Option[Int] = None, // timestamp in footer
    // metadata
    mrkdwn_in: Seq[String] = Seq.empty // list of strings where to apply markdown: title/text/pretext/fields
  )

  case class Message(
    text: Option[String] = None, // main message text
    username: Option[String] = None, // sender of message, default is WebHook name
    icon_emoji: Option[String] = None, // message icon, default is WebHook icon
    mrkdwn: Boolean = false, // apply or not Slack markdown to text
    parse: String = "none", // full/none parse of incoming message (full also implicits link_names=1)
    link_names: Boolean = false, // treat @<name> and #<name> as link to user/group (false = disable)
    attachments: Seq[Attachment] = Seq.empty
  )

  // generic response
  case class Response(code: Int, message: String)

  // [writes] ==========================================================================================================

  implicit val _AttachmentFieldW: Writes[AttachmentField] = Json.writes[AttachmentField]
  implicit val _AttachmentActionConfirmW: Writes[AttachmentActionConfirm] = Json.writes[AttachmentActionConfirm]
  implicit val _AttachmentActionW: Writes[AttachmentAction] = Json.writes[AttachmentAction]
  implicit val _AttachmentW: Writes[Attachment] = Json.writes[Attachment]
  implicit val _MessageW: Writes[Message] = Json.writes[Message]
}
