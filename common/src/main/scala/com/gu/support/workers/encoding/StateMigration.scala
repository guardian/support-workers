package com.gu.support.workers.encoding

import io.circe.{Json, parser}

/**
 * This object is responsible for migrating the previous version of the support-models state schema
 * to the current version
 */
object StateMigration {

  def migrate(oldValue: String): String = {
    val json = parser.parse(oldValue.toString).right.get
    val contribution = json \\ "contribution"
    if (contribution.nonEmpty)
      convertToProduct(contribution.head)
    else
      oldValue
  }

  private def convertToProduct(contributionJson: Json) = Contrib
}
