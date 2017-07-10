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

package uk.gov.hmrc.taasdummyexample

import java.io.File

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api.Mode._
import play.api.mvc.Request
import play.api.{Application, Configuration, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.filters.FrontendAuditFilter
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode, ServicesConfig}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.http.logging.filters.FrontendLoggingFilter
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import play.api.i18n.Messages.Implicits._
import play.api.Play.current
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws.{WSGet, WSHttp}
import uk.gov.hmrc.renderer.{MustacheRenderer, MustacheRendererTrait}
import uk.gov.hmrc.taasdummyexample.FrontendGlobal.baseUrl


object FrontendGlobal
  extends DefaultFrontendGlobal with ServicesConfig {

  override val auditConnector = FrontendAuditConnector
  override val loggingFilter = LoggingFilter
  override val frontendAuditFilter = AuditFilter
  lazy val localTemplateRenderer = LocalTemplateRenderer.localTemplateRenderer

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html = {
    implicit val hc = HeaderCarrier.fromHeadersAndSession(rh.headers, Some(rh.session))
    uk.gov.hmrc.taasdummyexample.views.html.error_template(pageTitle, heading, message)
  }

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object LoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = false
}

object AuditFilter extends FrontendAuditFilter with RunMode with AppName with MicroserviceFilterSupport {

  override lazy val maskedFormFields = Seq("password")

  override lazy val applicationPort = None

  override lazy val auditConnector = FrontendAuditConnector

  override def controllerNeedsAuditing(controllerName: String) = false
}

object WSAll extends WSHttp {
  override val hooks: Seq[HttpHook] = Seq()
}

object LocalTemplateRenderer extends ServicesConfig {
  lazy val localTemplateRenderer = new MustacheRenderer(
    WSAll,
    baseUrl("frontend-template-provider") + "/frontend-template-provider/serve-template"
  )
}