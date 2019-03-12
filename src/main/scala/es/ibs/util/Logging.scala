package es.ibs.util

import com.typesafe.scalalogging.Logger

trait Logging {

  protected val logger: Logger

  private def mm(m: Seq[Any]) = m.mkString(" ")

  def LOG_D1(m: Any): Unit = logger debug mm(Seq(m))
  def LOG_I1(m: Any): Unit = logger info mm(Seq(m))
  def LOG_W1(m: Any): Unit = logger warn mm(Seq(m))
  def LOG_E1(m: Any): Unit = logger error mm(Seq(m))
  def LOG_EX(e: Throwable): Unit = logger error(e.getMessage, e)

  def LOG_D(m: Any*): Unit = logger debug mm(m)
  def LOG_I(m: Any*): Unit = logger info mm(m)
  def LOG_W(m: Any*): Unit = logger warn mm(m)
  def LOG_E(m: Any*): Unit = logger error mm(m)
  def LOG_E(e: Throwable, m: Any*): Unit = logger error(mm(m), e)

  def catched(m: Any*)(block: =>Any = ()): PartialFunction[Throwable, Any] = {
    case err: Throwable =>
      LOG_E(err, m)
      block
  }

}
