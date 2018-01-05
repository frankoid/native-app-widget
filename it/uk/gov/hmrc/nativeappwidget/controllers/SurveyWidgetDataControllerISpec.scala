/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.nativeappwidget.controllers

import java.util.UUID

import org.scalatest.concurrent.Eventually
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.nativeappwidget.models.{Content, KeyValuePair}
import uk.gov.hmrc.nativeappwidget.repos.{SurveyResponseMongoRepository, SurveyWidgetRepository}
import uk.gov.hmrc.nativeappwidget.stubs.AuthStub
import uk.gov.hmrc.nativeappwidget.support.BaseISpec

import scala.collection.immutable
import scala.concurrent.ExecutionContext.Implicits.global

class SurveyWidgetDataControllerISpec extends BaseISpec with Eventually {
  private val campaignId = "TEST_CAMPAIGN_1"

  override protected def appBuilder: GuiceApplicationBuilder = super.appBuilder
    .configure("widget.surveys" -> Seq(campaignId))

  private val validSurveyData: JsObject = Json.obj(
    "campaignId" -> campaignId,
    "surveyData" -> Json.arr(
      Json.obj(
        "key" -> "question_1",
        "value" -> Json.obj(
          "content" -> "true",
          "contentType" -> "Boolean",
          "additionalInfo" -> "Would you like us to contact you?"
        )
      ),
      Json.obj(
        "key" -> "question_2",
        "value" -> Json.obj(
          "content" -> "John Doe",
          "contentType" -> "String",
          "additionalInfo" -> "What is your full name?"
        )
      )
    )
  )

  private lazy val surveyResponseRepository: SurveyResponseMongoRepository = app.injector.instanceOf[SurveyResponseMongoRepository]

  private def aPostSurveyResponseEndpoint(url: String): Unit = {
    "store survey data in mongo against the user's internal auth ID" in {
      val internalAuthid = s"Test-${UUID.randomUUID().toString}}"
      AuthStub.authoriseWithoutPredicatesWillReturnInternalId(internalAuthid)
      val response = await(wsUrl(url).post(validSurveyData))
      response.status shouldBe 200

      try {
        eventually {
          val storedSurveyDatas: immutable.Seq[SurveyWidgetRepository.SurveyResponsePersist] = await(surveyResponseRepository.find(
            "internalAuthid" -> internalAuthid))
          storedSurveyDatas.size shouldBe 1
          val storedSurveyData = storedSurveyDatas.head
          storedSurveyData.campaignId shouldBe campaignId
          storedSurveyData.surveyData shouldBe List(
            KeyValuePair("question_1", Content(content = "true", contentType = Some("Boolean"), additionalInfo = Some("Would you like us to contact you?"))),
            KeyValuePair("question_2", Content(content = "John Doe", contentType = Some("String"), additionalInfo = Some("What is your full name?")))
          )
        }
      }
      finally {
        surveyResponseRepository.remove("internalAuthid" -> internalAuthid)
      }
    }
  }

  "POST /native-app-widget/widget-data" should {
    behave like aPostSurveyResponseEndpoint("/native-app-widget/widget-data")
  }

  // old, deprecated URL - to be removed once native-apps-api-orchestration has been changed to use the new URL
  "POST /native-app-widget/:nino/widget-data" should {
    behave like aPostSurveyResponseEndpoint("/native-app-widget/CS700100A/widget-data")
  }

  "GET /native-app-widget/widget-data/:campaignId/:key" should {
    "retrieve the answer that was stored for a question" in {
      val internalAuthid = s"Test-${UUID.randomUUID().toString}}"
      AuthStub.authoriseWithoutPredicatesWillReturnInternalId(internalAuthid)
      val postSurveyDataResponse = await(wsUrl("/native-app-widget/CS700100A/widget-data").post(validSurveyData))
      postSurveyDataResponse.status shouldBe 200

      try {
        eventually {
          val getQuestion1Response = await(wsUrl(s"/native-app-widget/$campaignId/widget-data/question_1").get())
          getQuestion1Response.status shouldBe 200

          getQuestion1Response.json \ "content" shouldBe "true"
          getQuestion1Response.json \ "contentType" shouldBe "Boolean"
          getQuestion1Response.json \ "additionalInfo" shouldBe "Would you like us to contact you?"

          val getQuestion2Response = await(wsUrl(s"/native-app-widget/$campaignId/widget-data/question_1").get())
          getQuestion2Response.status shouldBe 200

          surveyResponseRepository.remove("internalAuthid" -> internalAuthid)
          getQuestion2Response.json \ "content" shouldBe "John Doe"
          getQuestion2Response.json \ "contentType" shouldBe "String"
          getQuestion2Response.json \ "additionalInfo" shouldBe "What is your full name?"
        }
      }
      finally {
        surveyResponseRepository.remove("internalAuthid" -> internalAuthid)
      }
    }

    "return 404 for a non-existent campaign" is pending
    "return 404 for a non-existent question" is pending
  }
}
