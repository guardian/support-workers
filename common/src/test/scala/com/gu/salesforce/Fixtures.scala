package com.gu.salesforce

object Fixtures {
  val idId = "3111123"
  val salesforceId = "0036E00000ImlhfQAB"
  val email = "yjcysqxfcqqytuzupjc@gu.com"
  val name = "YJCysqXFCqqYtuzuPJc"
  val uk = "UK"
  val us = "US"
  val state = "CA"
  val allowMail = false
  val upsertJson =
    s"""{
      "newContact": {
        "IdentityID__c": "$idId",
        "Email": "$email",
        "FirstName": "$name",
        "LastName": "$name",
        "MailingCountry": "$uk",
        "Allow_Membership_Mail__c": $allowMail,
        "Allow_3rd_Party_Mail__c": $allowMail,
        "Allow_Guardian_Related_Mail__c": $allowMail
       }
      }"""
  val upsertJsonWithState =
    s"""{
      "newContact": {
        "IdentityID__c": "$idId",
        "Email": "$email",
        "FirstName": "$name",
        "LastName": "$name",
        "MailingState": "$state",
        "MailingCountry": "$us",
        "Allow_Membership_Mail__c": $allowMail,
        "Allow_3rd_Party_Mail__c": $allowMail,
        "Allow_Guardian_Related_Mail__c": $allowMail
       }
      }"""
  val authJson =
    """
      {
        "access_token": "00Dg0000006RDAM!AREAQKDFKQ.ZPdIxWp4Z55tyVgs0D_kPhaiCMndEOk7WVB8yRffLVNK9TFbtZk34cWAfaaeojHL2ndURQounCzhRfBE_nMct",
        "instance_url": "https://cs17.salesforce.com",
        "id": "https://test.salesforce.com/id/00Dg0000006RDAMEA4/00520000003DLCDAA4",
        "token_type": "Bearer",
        "issued_at": "%d",
        "signature": "UK0fYmoyyuefxqsXFnovXy/RM/MleImPqZcf72ax+As="
      }
    """

  val expiredTokenResponse =
    """
      [{"message":"Session expired or invalid","errorCode":"INVALID_SESSION_ID"}]
    """

  val authenticationErrorResponse =
    """
      400: {"error":"invalid_client_id","error_description":"client identifier invalid"}
    """
}
