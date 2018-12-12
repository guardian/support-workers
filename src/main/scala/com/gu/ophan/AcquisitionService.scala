package com.gu.ophan

import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.gu.acquisition.services.DefaultAcquisitionServiceConfig
import com.gu.config.Configuration
import com.gu.okhttp.RequestRunners

object AcquisitionService {

  def apply(isTestService: Boolean): com.gu.acquisition.services.AcquisitionService =
    if (isTestService) {
      com.gu.acquisition.services.MockAcquisitionService
    } else {

      val credentialsProvider = new AWSCredentialsProviderChain(
        new ProfileCredentialsProvider("membership"),
        InstanceProfileCredentialsProvider.getInstance()
      )

      val config = DefaultAcquisitionServiceConfig(
        credentialsProvider,
        kinesisStreamName = Configuration.kinesisStreamName
      )
      com.gu.acquisition.services.AcquisitionService.prod(config)(RequestRunners.client)
    }
}
