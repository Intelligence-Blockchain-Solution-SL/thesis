package es.ibs.util

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

trait Converters {
  import Converters._

  private val hexArray = "0123456789abcdef".toCharArray

  implicit class ByteArrayConverters(bytes: Array[Byte]) {
    @inline final def md5: Array[Byte] = _digest(MD5, bytes)
    @inline final def sha256: Array[Byte] = _digest(SHA256, bytes)
    @inline final def sha512: Array[Byte] = _digest(SHA512, bytes)

    @inline final def hmac_md5(key: Array[Byte]): Array[Byte] = _mac(HMAC_MD5, key, bytes)
    @inline final def hmac_sha256(key: Array[Byte]): Array[Byte] = _mac(HMAC_SHA256, key, bytes)
    @inline final def hmac_sha512(key: Array[Byte]): Array[Byte] = _mac(HMAC_SHA512, key, bytes)

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
    @inline final def getBytesUTF8: Array[Byte] = str.getBytes(StandardCharsets.UTF_8)

    @inline final def md5: Array[Byte] = _digest(MD5, str.getBytesUTF8)
    @inline final def sha256: Array[Byte] = _digest(SHA256, str.getBytesUTF8)
    @inline final def sha512: Array[Byte] = _digest(SHA512, str.getBytesUTF8)

    @inline final def hmac_md5(key: Array[Byte]): Array[Byte] = _mac(HMAC_MD5, key, str.getBytesUTF8)
    @inline final def hmac_sha256(key: Array[Byte]): Array[Byte] = _mac(HMAC_SHA256, key, str.getBytesUTF8)
    @inline final def hmac_sha512(key: Array[Byte]): Array[Byte] = _mac(HMAC_SHA512, key, str.getBytesUTF8)

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

private object Converters {
  private val MD5 = "MD5"
  private val SHA256 = "SHA-256"
  private val SHA512 = "SHA-512"

  private val HMAC_MD5 = "HmacMD5"
  private val HMAC_SHA256 = "HmacSHA256"
  private val HMAC_SHA512 = "HmacSHA512"

  @inline private def _digest(algo: String, bytes: Array[Byte]) = MessageDigest.getInstance(algo).digest(bytes)

  @inline private def _mac(algo: String, key: Array[Byte], bytes: Array[Byte]) = pro(Mac.getInstance(algo)) { mac =>
    mac.init(new SecretKeySpec(key, algo))
    mac.doFinal(bytes)
  }
}