package com.gu.zuora.encoding

import java.util.UUID

import com.gu.helpers.StringExtensions._
import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.support.workers.encoding.Codec
import com.gu.support.workers.encoding.Helpers.{capitalizingCodec, deriveCodec}
import com.gu.support.workers.model._
import com.typesafe.scalalogging.LazyLogging
import io.circe._
import org.joda.time.{DateTime, LocalDate}

import scala.util.Try

object CustomCodecs extends CustomCodecs with ModelsCodecs with InternationalisationCodecs with HelperCodecs

trait InternationalisationCodecs {

  implicit val encodeCountryAsAlpha2: Encoder[Country] = Encoder.encodeString.contramap[Country](_.alpha2)
  implicit val decodeCountry: Decoder[Country] =
    Decoder.decodeString.emap { code => CountryGroup.countryByCode(code).toRight(s"Unrecognised country code '$code'") }
}

trait ModelsCodecs extends LazyLogging {
  self: CustomCodecs with InternationalisationCodecs with HelperCodecs =>

  implicit val encodeCurrency: Encoder[Currency] = Encoder.encodeString.contramap[Currency](_.iso)

  implicit val decodeCurrency: Decoder[Currency] =
    Decoder.decodeString.emap { code => Currency.fromString(code).toRight(s"Unrecognised currency code '$code'") }

  //PaymentMethods
  implicit val codecPayPalReferenceTransaction: Codec[PayPalReferenceTransaction] = capitalizingCodec

  implicit val codecCreditCardReferenceTransaction: Codec[CreditCardReferenceTransaction] = capitalizingCodec

  implicit val encodePaymentMethod: Encoder[PaymentMethod] = new Encoder[PaymentMethod] {
    override final def apply(a: PaymentMethod): Json = a match {
      case p: PayPalReferenceTransaction => Encoder[PayPalReferenceTransaction].apply(p)
      case c: CreditCardReferenceTransaction => Encoder[CreditCardReferenceTransaction].apply(c)
    }
  }

  implicit val decodePaymentMethod: Decoder[PaymentMethod] =
    Decoder[PayPalReferenceTransaction].map(x => x: PaymentMethod).or(
      Decoder[CreditCardReferenceTransaction].map(x => x: PaymentMethod)
    )
  //end

  implicit val decodePeriod: Decoder[Period] =
    Decoder.decodeString.emap{code => Period.fromString(code).toRight(s"Unrecognised period code '$code'")}

  implicit val encodePeriod: Encoder[Period] = Encoder.encodeString.contramap[Period](_.toString)

  //Products
  implicit val codecDigitalBundle: Codec[DigitalBundle] = deriveCodec
  implicit val codecContribution: Codec[Contribution] = deriveCodec

  implicit val encodeProduct: Encoder[ProductType] = new Encoder[ProductType] {
    override final def apply(a: ProductType): Json = a match {
      case d: DigitalBundle => Encoder[DigitalBundle].apply(d).mapObject(json => json.add("type", Json.fromString(d.toString())))
      case c: Contribution => Encoder[Contribution].apply(c).mapObject(json => json.add("type", Json.fromString(c.toString())))
    }
  }

  implicit val decodeProduct: Decoder[ProductType] = Decoder.instance(c =>
    c.downField("type").as[String].right.get match {
      case "Contribution" => c.as[Contribution]
      case "DigitalBundle" => c.as[DigitalBundle]
    }
  )
  //end

  //PaymentFields
  implicit val codecPayPalPaymentFields: Codec[PayPalPaymentFields] = capitalizingCodec
  implicit val codecStripePaymentFields: Codec[StripePaymentFields] = capitalizingCodec
  implicit val codecDirectDebitPaymentFields: Codec[DirectDebitPaymentFields] = capitalizingCodec

  implicit val encodePaymentFields: Encoder[PaymentFields] = new Encoder[PaymentFields] {
    override final def apply(a: PaymentFields): Json = a match {
      case p: PayPalPaymentFields => Encoder[PayPalPaymentFields].apply(p)
      case c: StripePaymentFields => Encoder[StripePaymentFields].apply(c)
      case d: DirectDebitPaymentFields => Encoder[DirectDebitPaymentFields].apply(d)
    }
  }

  implicit val decodePaymentFields: Decoder[PaymentFields] =
    Decoder[PayPalPaymentFields].map(x => x: PaymentFields)
      .or(Decoder[StripePaymentFields].map(x => x: PaymentFields))
      .or(Decoder[DirectDebitPaymentFields].map(x => x: PaymentFields))
  //end

  implicit val codecUser: Codec[User] = deriveCodec
}

trait HelperCodecs {
  implicit val encodeLocalTime: Encoder[LocalDate] = Encoder.encodeString.contramap(_.toString("yyyy-MM-dd"))
  implicit val decodeLocalTime: Decoder[LocalDate] = Decoder.decodeString.map(LocalDate.parse)
  implicit val encodeDateTime: Encoder[DateTime] = Encoder.encodeLong.contramap(_.getMillis)
  implicit val decodeDateTime: Decoder[DateTime] = Decoder.decodeLong.map(new DateTime(_))
  implicit val uuidDecoder: Decoder[UUID] =
    Decoder.decodeString.emap { code =>
      Try {
        UUID.fromString(code)
      }.toOption.toRight(s"Invalid UUID '$code'")
    }

  implicit val uuidEnecoder: Encoder[UUID] = Encoder.encodeString.contramap(_.toString)
}

trait CustomCodecs {
  //Request encoders
  //Account object encoding - unfortunately the custom field name of the Salesforce contact id starts with a lower case
  //letter whereas all the other fields start with upper case so we need to set it explicitly
  private val salesforceIdName = "sfContactId__c"

  def decapitalizeSfContactId(fieldName: String): String =
    if (fieldName == salesforceIdName.capitalize) salesforceIdName.decapitalize
    else fieldName
}