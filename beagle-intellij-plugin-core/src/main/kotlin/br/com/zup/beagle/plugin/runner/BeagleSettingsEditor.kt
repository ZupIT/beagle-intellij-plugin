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

package br.com.zup.beagle.plugin.runner

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.lang.StringUtils
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

open class BeagleSettingsEditor(private var content: JPanel, private var methodNameInput: JTextField,
                                private var enableHotReloadOnFileSaved: JCheckBox, private var labelClass: JLabel
                                ) : SettingsEditor<BeagleRunConfiguration>() {

    private var selectedFile: VirtualFile? = null

    override fun resetEditorFrom(beagleRunConfiguration: BeagleRunConfiguration) {
        if (beagleRunConfiguration.clazzToRunPlugin != null) {
            this.selectedFile = beagleRunConfiguration.clazzToRunPlugin
            this.labelClass.text = (beagleRunConfiguration.clazzToRunPlugin as VirtualFile).name
            this.methodNameInput.text = beagleRunConfiguration.methodName
            this.enableHotReloadOnFileSaved.isSelected = beagleRunConfiguration.enableHotReloadOnFileSaved
        } else {
            this.selectedFile = null
            this.labelClass.text = StringUtils.EMPTY
            this.methodNameInput.text = null
            this.enableHotReloadOnFileSaved.isSelected = true
        }
    }

    override fun createEditor() = this.content

    override fun applyEditorTo(beagleRunConfiguration: BeagleRunConfiguration) {
        beagleRunConfiguration.clazzToRunPlugin = this.selectedFile
        beagleRunConfiguration.methodName = this.methodNameInput.text
        beagleRunConfiguration.enableHotReloadOnFileSaved = this.enableHotReloadOnFileSaved.isSelected
    }
}