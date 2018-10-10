package com.gu.zuora

import java.time.{LocalDateTime, ZoneId, ZonedDateTime}

import cats.data.OptionT
import cats.implicits._
import com.gu.helpers.WebServiceHelper
import com.gu.okhttp.RequestRunners.FutureHttpClient
import com.gu.zuora.GetAccountForIdentity.DomainAccount
import com.gu.zuora.model.response._
import com.gu.zuora.model.{QueryData, SubscribeRequest}
import io.circe
import io.circe.Decoder
import io.circe.parser.decode
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}

class ZuoraService(val config: ZuoraConfig, client: FutureHttpClient, baseUrl: Option[String] = None)(implicit ec: ExecutionContext)
    extends WebServiceHelper[ZuoraErrorResponse] {

  override val wsUrl: String = baseUrl.getOrElse(config.url)
  override val httpClient: FutureHttpClient = client
  val authHeaders = Map(
    "apiSecretAccessKey" -> config.password,
    "apiAccessKeyId" -> config.username
  )

  def getAccount(accountNumber: String): Future[GetAccountResponse] =
    get[GetAccountResponse](s"accounts/$accountNumber", authHeaders)

  def getAccountFields(identityId: String): Future[List[DomainAccount]] = {
    val queryData = QueryData(s"select AccountNumber, CreatedDate from account where IdentityId__c = '${identityId.toLong}'")
    postJson[AccountQueryResponse](s"action/query", queryData.asJson, authHeaders).map(_.records.map(DomainAccount.fromAccountRecord))
  }

  def getSubscriptions(accountId: String): Future[List[Subscription]] =
    get[SubscriptionsResponse](s"subscriptions/accounts/$accountId", authHeaders).map(_.subscriptions)

  def subscribe(subscribeRequest: SubscribeRequest): Future[List[SubscribeResponseAccount]] =
    postJson[List[SubscribeResponseAccount]]("action/subscribe", subscribeRequest.asJson, authHeaders)

  def getDefaultPaymentMethodId(accountNumber: String): Future[Option[String]] = {
    val queryData = QueryData(s"select defaultPaymentMethodId from Account where AccountNumber = '$accountNumber'")
    postJson[PaymentMethodQueryResponse](s"action/query", queryData.asJson, authHeaders)
      .map(r => Some(r.records.head.DefaultPaymentMethodId))
      .fallbackTo(Future.successful(None))
  }

  def getDirectDebitMandateId(paymentMethodId: String): Future[Option[String]] = {
    get[PaymentMethodDetailResponse](s"object/payment-method/$paymentMethodId", authHeaders)
      .map(p => Some(p.MandateID))
      .fallbackTo(Future.successful(None))
  }

  def getMandateIdFromAccountNumber(accountNumber: String): Future[Option[String]] = {
    (for {
      pmId <- OptionT(getDefaultPaymentMethodId(accountNumber))
      ddId <- OptionT(getDirectDebitMandateId(pmId))
    } yield ddId).value
  }

  override def decodeError(responseBody: String)(implicit errorDecoder: Decoder[ZuoraErrorResponse]): Either[circe.Error, ZuoraErrorResponse] =
    //The Zuora api docs say that the subscribe action returns
    //a ZuoraErrorResponse but actually it returns a list of those.
    decode[List[ZuoraErrorResponse]](responseBody).map(_.head)

}

object GetAccountForIdentity {

  case class DomainAccount(accountNumber: String, createdDate: LocalDateTime)

  object DomainAccount {

    def fromAccountRecord(accountRecord: AccountRecord): DomainAccount =
      DomainAccount(
        accountRecord.AccountNumber,
        ZonedDateTime.parse(accountRecord.CreatedDate).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime
      )

  }

}