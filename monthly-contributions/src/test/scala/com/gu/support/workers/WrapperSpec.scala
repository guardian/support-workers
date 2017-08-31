package com.gu.support.workers

import com.gu.support.workers.Fixtures.{contributionJson, wrapFixture}
import com.gu.support.workers.encoding.Encoding
import com.gu.support.workers.model.Contribution
import com.gu.zuora.encoding.CustomCodecs._
import org.scalatest.{FlatSpec, Matchers}

class WrapperSpec extends FlatSpec with Matchers {
  "Wrapper" should "be able to round trip some json" in {
    val wrapped = wrapFixture(contributionJson)

    val contribution = Encoding.in[Contribution](wrapped)
    contribution.isSuccess should be(true)
  }
}
