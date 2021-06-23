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

package br.com.zup.beagle.plugin

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JComponent.*
import javax.swing.JLabel
import javax.swing.JPanel


class Teste : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel()
//        val browser = WebView()
//        browser.navigate("http://www.google.com")
        panel.add(JLabel())

        var contentFactory: ContentFactory? = ContentFactory.SERVICE.getInstance()
        var content: com.intellij.ui.content.Content? = contentFactory?.createContent(panel, "", false)
        content?.let {
            toolWindow.getContentManager().addContent(it)
        }
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.stripeTitle = "Beaglezada"
    }

    // Only show in Android projects.
    override fun shouldBeAvailable(project: Project): Boolean {
        return true
    }
}