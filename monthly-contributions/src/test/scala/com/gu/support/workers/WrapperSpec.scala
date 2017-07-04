package com.gu.support.workers

import com.gu.support.workers.Fixtures.{contributionJson, wrap}
import com.gu.support.workers.encoding.Encoding
import com.gu.support.workers.model.monthlyContributions.Contribution
import com.gu.zuora.encoding.CustomCodecs.codecContribution
import org.scalatest.{FlatSpec, Matchers}

class WrapperSpec extends FlatSpec with Matchers {
  "Wrapper" should "be able to round trip some json" in {
    val wrapped = wrap(contributionJson)

    val contribution = Encoding.in[Contribution](wrapped)
    contribution.isSuccess should be(true)
  }
}
