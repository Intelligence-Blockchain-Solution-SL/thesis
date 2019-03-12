package es.ibs

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

package object util extends Converters {

  @inline final def NOW: Int = (System.currentTimeMillis() / 1000).toInt

  @inline final def con[T, S](obj: T)(op: T => S): T = {
    op(obj)
    obj
  }

  @inline final def ifdef[A](cond: => Boolean)(mk: => A): Option[A] = {
    if (cond) {
      Some(mk)
    } else {
      None
    }
  }

  @inline final def using[T <: { def close(): Unit }, S](obj: T)(op: T => S): S = {
    import scala.language.reflectiveCalls

    try {
      op(obj)
    } finally {
      obj.close()
    }
  }

  @inline final def time[R](block: (=>Long) => R): R = {
    val t0 = System.nanoTime()
    block { (System.nanoTime() - t0) / 1000000000 }
  }

  def sequentialExecution[A, B](seq: Seq[A])(f: A => Future[B])(implicit e: ExecutionContext): Future[Seq[B]] = {
    seq.foldLeft(Future.successful(Seq[B]())) { case (left, next) =>
      left.flatMap(res => f(next).map(x => res :+ x))
    }
  }

  // -- PIMPs simple ---------------------------------------------------------------------------------------------------

  implicit class TernaryBoolean(condition: Boolean) {
    @inline final def ?[T](trueVal: =>T): JustTrue[T] =
      new JustTrue(condition, trueVal)

    final class JustTrue[T](cond: Boolean, trueVal: =>T) {
      @inline def |(falseVal: =>T): T =
        if (cond) trueVal else falseVal
    }
  }

  implicit class SeqEx[A](seq: Seq[A]) {
    @inline final def takeRandomN(n: Int): Seq[A] = scala.util.Random.shuffle(seq).take(n)
  }

  implicit class SeqBigDecimal[A](seq: Seq[BigDecimal]) {
    @inline final def average: BigDecimal = seq.foldLeft((BigDecimal(0.0), 1)) {
      case ((avg, idx), next) => (avg + (next - avg) / idx, idx + 1)
    }._1
  }

  // -- PIMPs collections ----------------------------------------------------------------------------------------------

  implicit class SeqFuture[A](seq: Seq[Future[A]]) {
    // decorator for Future.sequence(Seq(fut1, fut2, fut2, ...)) ::= Seq(fut1, fut2, fut2, ...).fut
    @inline final def fut(implicit ex: ExecutionContext) = Future.sequence(seq)
  }
}
