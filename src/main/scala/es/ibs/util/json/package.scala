package es.ibs.util

import scala.collection.Seq

import play.api.libs.json._

package object json {

  // == UNIT ===========================================================================================================

  implicit object UnitFormat extends Format[Unit] {
    def reads(json: JsValue): JsResult[Unit] = json match {
      case JsNull => JsSuccess(Unit)
      case _ => JsError(Seq(JsPath -> Seq(JsonValidationError("error.expected.jsnull"))))
    }
    def writes(obj: Unit): JsValue = JsNull
  }

  // == CHAR ===========================================================================================================

  implicit object CharFormat extends Format[Char] {
    def reads(json: JsValue): JsResult[Char] = json match {
      case JsString(value) if value.length == 1 => JsSuccess(value.head)
      case _ => JsError(Seq(JsPath -> Seq(JsonValidationError("error.expected.jsstring(len=1)"))))
    }
    def writes(obj: Char): JsValue = JsString(obj.toString)
  }

  // == EITHER =========================================================================================================

  implicit def eitherReads[A : Reads, B : Reads](implicit A: Reads[A], B: Reads[B]): Reads[Either[A, B]] = Reads[Either[A, B]] { json =>
    A.reads(json) match {
      case JsSuccess(value, path) => JsSuccess(Left(value), path)
      case JsError(e1) => B.reads(json) match {
        case JsSuccess(value, path) => JsSuccess(Right(value), path)
        case JsError(e2) => JsError(JsError.merge(e1, e2))
      }
    }
  }

  implicit def eitherWrites[A, B](implicit A: Writes[A], B: Writes[B]): Writes[Either[A, B]] = Writes[Either[A, B]] {
    case Left(a) => A.writes(a)
    case Right(b) => B.writes(b)
  }

  implicit def eitherFormat[A, B](implicit A: Format[A], B: Format[B]): Format[Either[A,B]] = Format(eitherReads, eitherWrites)

  // == ENUMERATION (addition to Reads.enumNameReads(E) and  Writes.enumNameWrites) ====================================

  def enumNameFormat[E <: Enumeration](enum: E): Format[E#Value] = Format(Reads.enumNameReads(enum), Writes.enumNameWrites)

  def enumIdReads[E <: Enumeration](enum: E): Reads[E#Value] = {
    case JsNumber(id) if id.isValidInt =>
      enum.values
        .find(_.id == id.toInt)
        .map(JsSuccess(_))
        .getOrElse(JsError(Seq(JsPath -> Seq(JsonValidationError("error.expected.validenumid")))))
    case _ => JsError(Seq(JsPath -> Seq(JsonValidationError("error.expected.enumid"))))
  }

  def enumIdWrites[E <: Enumeration]: Writes[E#Value] =
    Writes[E#Value] { value: E#Value => JsNumber(value.id) }

  def enumIdFormat[E <: Enumeration](enum: E): Format[E#Value] = Format(enumIdReads(enum), enumIdWrites)

  def enumSetReads[E <: Enumeration](enum: E): Reads[E#ValueSet] = {
    case JsNumber(ids) if ids.isValidLong =>
      val bs = collection.immutable.BitSet.fromBitMask(Array(ids.toLong))
      enum.values.filter(v => bs.contains(v.id)) match {
        case vs: E#ValueSet if vs.size == bs.size => JsSuccess(vs)
        case _ => JsError(Seq(JsPath -> Seq(JsonValidationError("error.expected.validenumset"))))
      }
    case _ => JsError(Seq(JsPath -> Seq(JsonValidationError("error.expected.enumid"))))
  }

  def enumSetWrites[E <: Enumeration]: Writes[E#ValueSet] =
    Writes[E#ValueSet] { value: E#ValueSet => JsNumber(value.toBitMask.sum) }

  def enumSetFormat[E <: Enumeration](enum: E): Format[E#ValueSet] = Format(enumSetReads(enum), enumSetWrites)

}
