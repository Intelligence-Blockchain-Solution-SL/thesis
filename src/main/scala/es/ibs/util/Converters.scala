package es.ibs.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

trait Converters {

  private val hexArray = "0123456789abcdef".toCharArray

  implicit class ByteArrayConverters(bytes: Array[Byte]) {

    @inline private def _digest(algo: String) = MessageDigest.getInstance(algo).digest(bytes)

    @inline final def md5: Array[Byte] = _digest("MD5")
    @inline final def sha256: Array[Byte] = _digest("SHA-256")
    @inline final def sha512: Array[Byte] = _digest("SHA-512")

    @inline final def hex: String = {
      val hexChars = new Array[Char](bytes.length * 2)
      bytes.indices foreach { j =>
        val v = bytes(j) & 0xFF
        hexChars(j * 2) = hexArray(v >>> 4)
        hexChars(j * 2 + 1) = hexArray(v & 0x0F)
      }
      new String(hexChars)
    }

    @inline final def base64: String = Base64.getEncoder.encodeToString(bytes)
    @inline final def base64url: String = Base64.getUrlEncoder.withoutPadding().encodeToString(bytes)
  }

  implicit class StringConverters(str: String) {

    @inline private def _digest(algo: String) = MessageDigest.getInstance(algo).digest(str.getBytes(StandardCharsets.UTF_8))

    @inline final def md5: Array[Byte] = _digest("MD5")
    @inline final def sha256: Array[Byte] = _digest("SHA-256")
    @inline final def sha512: Array[Byte] = _digest("SHA-512")

    @inline final def md5_hex: String = md5.hex
    @inline final def sha256_hex: String = sha256.hex
    @inline final def sha512_hex: String = sha512.hex

    @inline final def urlEncode: String = URLEncoder.encode(str, StandardCharsets.UTF_8)

    @inline final def base64: Array[Byte] = Base64.getDecoder.decode(str)
    @inline final def base64url: Array[Byte] = Base64.getUrlDecoder.decode(str)
  }

  @inline final def hex2string(hex: String): String = {
    val sb = new StringBuilder
    (0 until hex.length by 2) foreach (i => sb.append(Integer.parseInt(hex.substring(i, i + 2), 16).toChar))
    sb.toString
  }

  implicit class RichOptional[T](opt: java.util.Optional[T]) {
    @inline def asScala: Option[T] = if(opt.isPresent) Some(opt.get()) else None
  }
}
