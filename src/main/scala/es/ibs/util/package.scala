package es.ibs

import scala.collection.Seq
import scala.concurrent.{ExecutionContext, Future}

package object util extends Converters {

  @inline final def NOW: Int = (System.currentTimeMillis() / 1000).toInt

  @inline final def ifdef[A](cond: => Boolean)(mk: => A): Option[A] = {
    if (cond) Some(mk) else None
  }

  @inline final def pro[T, S](obj: T)(op: T => S): S = {
    op(obj)
  }

  @inline final def con[T, S](obj: T)(op: T => S): T = {
    op(obj)
    obj
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

  def timeDiff(interval: Long, delim: String = " "): String = {
    val d = java.time.Duration.ofMillis(math.abs(interval))
    val (dd, hh, mi, ss) = (d.toDaysPart, d.toHoursPart, d.toMinutesPart, d.toSecondsPart)
    var f = false
    var s = ""
    if(dd > 0L) { s += s"${dd}d$delim"; f = true}
    if(hh > 0 || f) { s += s"${hh}h$delim"; f = true}
    if(mi > 0 || f) s += s"${mi}m$delim"
    s += s"${ss}s"
    if(interval < 0) "-" + s else s
  }

  def pctDiff(a: BigDecimal, b: BigDecimal): BigDecimal = (b - a) / a

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


  // -- PIMPs collections ----------------------------------------------------------------------------------------------

  implicit class SeqEx[T](seq: Seq[T]) {
    @inline final def takeUntil(predicate: T => Boolean): Seq[T] = {
      seq.span(predicate) match {
        case (head, tail) => head ++ tail.take(1)
      }
    }
    @inline final def takeRandomN(n: Int): Seq[T] =
      scala.util.Random.shuffle(seq).take(n)
  }

  implicit class SeqBigDecimal[A](seq: Seq[BigDecimal]) {
    @inline final def average: BigDecimal = seq.foldLeft((BigDecimal(0.0), 1)) {
      case ((avg, idx), next) => (avg + (next - avg) / idx, idx + 1)
    }._1
  }

  implicit class SeqFuture[A](seq: Seq[Future[A]]) {
    // decorator for Future.sequence(Seq(fut1, fut2, fut2, ...)) ::= Seq(fut1, fut2, fut2, ...).fut
    @inline final def fut(implicit ex: ExecutionContext): Future[Seq[A]] = Future.sequence(seq)
  }

  implicit class OptionCollectionFunction[T](collection: Iterable[T]) {
    @inline final def minByOption[C](f: T => C)(implicit cmp: Ordering[C]): Option[T] =
      if (collection.isEmpty) None else Some(collection.minBy(f))

    @inline final def maxByOption[C](f: T => C)(implicit cmp: Ordering[C]): Option[T] =
      if (collection.isEmpty) None else Some(collection.maxBy(f))

  }
}
