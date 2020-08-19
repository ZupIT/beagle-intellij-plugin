/*
 * Copyright 2020 ZUP IT SERVICOS EM TECNOLOGIA E INOVACAO SA
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

package br.com.zup.beagle.plugin.report

import com.intellij.diagnostic.IdeaReportingEvent
import com.intellij.ide.DataManager
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ErrorReportSubmitter
import com.intellij.openapi.diagnostic.IdeaLoggingEvent
import com.intellij.openapi.diagnostic.SubmittedReportInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.util.Consumer
import java.awt.Component


class ErrorReport : ErrorReportSubmitter() {

    override fun getPrivacyNoticeText() = "Privacy configuration todo"

    override fun getReportActionText() = "Report to ZupIT"

    override fun submit(events: Array<out IdeaLoggingEvent>, additionalInfo: String?, parentComponent: Component, consumer: Consumer<SubmittedReportInfo>): Boolean {
        val context = DataManager.getInstance().getDataContext(parentComponent)
        val project = CommonDataKeys.PROJECT.getData(context)
        object : Task.Backgroundable(project, "Sending Error Report") {
            override fun run(indicator: ProgressIndicator) {
                submitReport(
                    indicator = indicator,
                    events = events,
                    parentComponent = parentComponent,
                    consumer = consumer
                )
            }
        }.queue()
        return true
    }

    private fun submitReport(indicator: ProgressIndicator, events: Array<out IdeaLoggingEvent>, parentComponent: Component, consumer: Consumer<SubmittedReportInfo>) {
        var version = "UNKNOWN"
        if (this.pluginDescriptor is IdeaPluginDescriptor) {
            version = (pluginDescriptor as IdeaPluginDescriptor).version
        }
        val errors = events.filterIsInstance<IdeaReportingEvent>().map {
            val data = it.data
            Triple(data.throwable, data.allAttachments, data.additionalInfo)
        }
        //todo create issue with recovered data
        println("Error = $version - $errors")
        ApplicationManager.getApplication().invokeLater {
            Messages.showInfoMessage(parentComponent, "Thank you for submitting your report!", "Error Report")
            consumer.consume(SubmittedReportInfo(SubmittedReportInfo.SubmissionStatus.NEW_ISSUE))
        }
    }
}