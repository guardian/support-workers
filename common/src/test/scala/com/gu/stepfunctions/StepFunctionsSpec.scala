package com.gu.stepfunctions

import com.amazonaws.regions.Regions
import com.amazonaws.services.stepfunctions.AWSStepFunctionsAsyncClientBuilder
import com.gu.aws.CredentialsProvider
import com.gu.test.tags.annotations.IntegrationTest
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{AsyncFlatSpec, Matchers}

@IntegrationTest
class StepFunctionsSpec extends AsyncFlatSpec with Matchers with LazyLogging {
  val client = AWSStepFunctionsAsyncClientBuilder.standard
    .withCredentials(CredentialsProvider)
    .withRegion(Regions.EU_WEST_1)
    .build()

  "StepFunctionsService" should "be able to find all state machines" in {
    val service = new StepFunctionsService()
    service.getStateMachines().map {
      stateMachineListItems =>
        stateMachineListItems.nonEmpty should be(true)
    }
  }

  it should "be able to find a user's data" in {
    val userId = "30002031"
    new StepFunctionsService().findUserData(userId).map {
      maybeUser =>
        maybeUser.isDefined should be(true)
        maybeUser.get.firstName should be("t4S8sP9vQwrElSv0wKc")
    }
  }
}
