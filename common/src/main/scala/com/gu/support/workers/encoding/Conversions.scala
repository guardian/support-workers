package com.gu.support.workers.encoding

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import cats.syntax.either._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

import scala.reflect.io.Streamable
import scala.util.Try

object Conversions {

  implicit class InputStreamConversions[T](val self: T) {

    def asInputStream()(implicit encoder: Encoder[T]): ByteArrayInputStream = {
      val convertStream = new ByteArrayOutputStream()
      convertStream.write(self.asJson.noSpaces.getBytes(utf8))
      convertStream.toInputStream
    }
  }

  implicit class FromOutputStream(val self: ByteArrayOutputStream) {
    def toClass[T](encrypted: Boolean)(implicit decoder: Decoder[T]): T = {
      val is = self.toInputStream
      val str = Encryption.decrypt(Streamable.bytes(is), encrypted)
      val t = Try(str).flatMap(decode[T](_).toTry)
      is.close()
      t.get
    }

    def toInputStream: ByteArrayInputStream = new ByteArrayInputStream(self.toByteArray)
  }

  implicit class StringInputStreamConversions[String](val str: String) {

    def asInputStream: ByteArrayInputStream = {
      val convertStream = new ByteArrayOutputStream()

      convertStream.write(str.toString.getBytes(utf8))
      new ByteArrayInputStream(convertStream.toByteArray)
    }
  }

}
