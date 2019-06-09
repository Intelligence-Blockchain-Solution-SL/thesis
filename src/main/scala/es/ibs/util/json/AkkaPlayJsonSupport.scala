package es.ibs.util.json

import scala.collection.immutable.Seq
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model.{ContentTypeRange, MediaType}
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import play.api.libs.json.{JsError, JsValue, Json, Reads, Writes}

/**
  * Automatic to and from JSON marshalling/unmarshalling using an in-scope *play-json* protocol.
  */
trait AkkaPlayJsonSupport {
  import AkkaPlayJsonSupport._

  def mediaTypes: Seq[MediaType.WithFixedCharset] = List(`application/json`)
  def unmarshallerContentTypes: Seq[ContentTypeRange] = mediaTypes.map(ContentTypeRange.apply)

  private val jsonStringUnmarshaller = Unmarshaller.byteStringUnmarshaller
    .forContentTypes(unmarshallerContentTypes: _*)
    .mapWithCharset {
      case (ByteString.empty, _) => throw Unmarshaller.NoContentException
      case (data, charset)       => data.decodeString(charset.nioCharset.name)
    }

  private val jsonStringMarshaller = Marshaller.oneOf(mediaTypes: _*)(Marshaller.stringMarshaller)

  /**
    * HTTP entity => `A`
    *
    * @tparam A type to decode
    * @return unmarshaller for `A`
    */
  implicit def unmarshaller[A: Reads]: FromEntityUnmarshaller[A] = {
    def read(json: JsValue) = implicitly[Reads[A]].reads(json).recoverTotal(e => throw PlayJsonError(e))
    jsonStringUnmarshaller.map(data => read(Json.parse(data)))
  }

  /**
    * `A` => HTTP entity
    *
    * @tparam A type to encode
    * @return marshaller for any `A` value
    */
  implicit def marshaller[A](implicit writes: Writes[A], printer: JsValue => String = Json.stringify): ToEntityMarshaller[A] =
    jsonStringMarshaller.compose(printer).compose(writes.writes)
}

/**
  * Automatic to and from JSON marshalling/unmarshalling using an in-scope play-json protocol.
  */
object AkkaPlayJsonSupport extends AkkaPlayJsonSupport {
  final case class PlayJsonError(error: JsError) extends IllegalArgumentException {
    override def getMessage: String = JsError.toJson(error).toString()
  }
}