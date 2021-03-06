/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.nativeappwidget.models

import play.api.libs.json.{Format, Json}

/**
  * Represents surveyData we are posted in the service request
  *
  * @param campaignId an ID which associates the surveyData to a particular survey
  * @param surveyData the questions and answers
  */
case class SurveyResponse(campaignId: String,
                          surveyData: List[KeyValuePair])

case class KeyValuePair(key: String, value: Content)

case class Content(content: String, contentType: Option[String], additionalInfo: Option[String])

object Content {
  implicit val contentFormat: Format[Content] = Json.format[Content]
}

object KeyValuePair {
  implicit val keyValuePairFormat: Format[KeyValuePair] = Json.format[KeyValuePair]
}

object SurveyResponse {
  implicit val dataFormat: Format[SurveyResponse] = Json.format[SurveyResponse]
}






