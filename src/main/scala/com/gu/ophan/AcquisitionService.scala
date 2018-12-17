package com.gu.ophan

import com.gu.acquisition.services.LambdaConfig
import com.gu.config.Configuration
import com.gu.okhttp.RequestRunners

object AcquisitionService {

  def apply(isTestService: Boolean): com.gu.acquisition.services.AcquisitionService =
    if (isTestService) {
      com.gu.acquisition.services.MockAcquisitionService
    } else {

      val config = LambdaConfig(
        kinesisStreamName = Configuration.kinesisStreamName
      )
      com.gu.acquisition.services.AcquisitionService.prod(config)(RequestRunners.client)
    }
}
