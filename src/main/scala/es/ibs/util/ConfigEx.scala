package es.ibs.util

import java.util.Properties
import java.util.concurrent.TimeUnit
import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import com.typesafe.config.{Config, ConfigObject, ConfigValue, ConfigValueType}


class ConfigEx(val c: Config) extends AnyVal {
  import scala.collection.JavaConverters._
  import ConfigEx._

  def getBooleanOrElse(path: String, default: => Boolean): Boolean = if(c.hasPath(path)) c.getBoolean(path) else default

  def getIntOrElse(path: String, default: => Int): Int = if(c.hasPath(path)) c.getInt(path) else default

  def getLongOrElse(path: String, default: => Long): Long = if(c.hasPath(path)) c.getLong(path) else default

  def getStringOrElse(path: String, default: => String): String = if(c.hasPath(path)) c.getString(path) else default

  def getMillisOrElse(path: String, default: => Long): Long = if(c.hasPath(path)) c.getDuration(path, TimeUnit.MILLISECONDS) else default

  def getDurationOrElse(path: String, default: => Duration): Duration =
    if(c.hasPath(path)) Duration(c.getDuration(path, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS) else default

  def getMap[A](path: String): Map[String, A] = {
    val list = c.getObjectList(path).asScala
    (for {
      item <- list
      entry <- item.entrySet().asScala
      key = entry.getKey // asInstanceOf[A]
      uri = entry.getValue.unwrapped().asInstanceOf[A]
    } yield (key, uri)).toMap
  }

  def getMapOrElse[A](path: String, default: => Map[String, A]): Map[String, A] = if(c.hasPath(path)) c.getMap(path) else default

  def getSeq[A](path: String): Seq[A] = c.getList(path).unwrapped().asScala.map(_.asInstanceOf[A])

  def getSeqOrElse[A](path: String, default: => Seq[A]): Seq[A] = if(c.hasPath(path)) c.getSeq(path) else default

  def getProperties(path: String): Properties = c.getConfig(path).toProperties

  def toProperties: Properties = {
    def toProps(m: mutable.Map[String, ConfigValue]): Properties = {
      con(new Properties(null)) { props =>
        m.foreach { case (k, cv) =>
          val v =
            if(cv.valueType() == ConfigValueType.OBJECT) toProps(cv.asInstanceOf[ConfigObject].asScala)
            else if(cv.unwrapped eq null) null
            else cv.unwrapped.toString
          if(v ne null) props.put(k, v)
        }
      }
    }
    toProps(c.root.asScala)
  }
}

object ConfigEx {
  @inline implicit def configExtensionMethods(c: Config): ConfigEx = new ConfigEx(c)
}