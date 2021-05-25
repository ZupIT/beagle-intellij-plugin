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

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.getOrCreate
import org.jdom.Element
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTextField

open class BeagleRunConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    ModuleBasedConfiguration<BeagleConfigurationModule, BeagleRunConfigurationType>(name, BeagleConfigurationModule(project), factory) {
    companion object {
        private const val BEAGLE_PREVIEW = "beaglePreview"
        private const val CLASS_FILE_PATH = "classFilePath"
        private const val METHOD_NAME = "methodName"
        private const val HOT_RELOAD = "enableHotReload"
    }

    var clazzToRunPlugin: VirtualFile? = null
    var methodName: String? = null
    var enableHotReloadOnFileSaved: Boolean = true
    var javaCommandLineState: BeagleJavaCommandLineState? = null

    override fun getConfigurationEditor() = BeagleSettingsEditor(this.project, JPanel(), TextFieldWithBrowseButton(), JTextField(), JCheckBox())

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        StopProcess.stopRunningCustomRunConfigurations(project)
        val commandLineState = BeagleJavaCommandLineState(environment, clazzToRunPlugin, methodName)
        val textConsoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(this.project)
        commandLineState.consoleBuilder = textConsoleBuilder
        this.javaCommandLineState = commandLineState
        return commandLineState
    }

    override fun suggestedName() = if (this.clazzToRunPlugin != null && this.methodName != null) {
        (this.type as BeagleRunConfigurationType).getRunConfigurationName((this.clazzToRunPlugin as VirtualFile).name, this.methodName!!)
    } else {
        this.name
    }

    override fun getValidModules() = listOf(this.configurationModule.module)

    override fun readExternal(element: Element) {
        super.readExternal(element)
        element.getChild(BEAGLE_PREVIEW)?.also { child: Element ->
            this.clazzToRunPlugin = child.getAttributeValue(CLASS_FILE_PATH)?.let(LocalFileSystem.getInstance()::findFileByPath)
            this.methodName = child.getAttributeValue(METHOD_NAME)
            this.enableHotReloadOnFileSaved = child.getAttributeValue(HOT_RELOAD) == true.toString()
        }
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.getOrCreate(BEAGLE_PREVIEW).also { child: Element ->
            child.setAttributeSafely(CLASS_FILE_PATH, this.clazzToRunPlugin?.path)
            child.setAttributeSafely(METHOD_NAME, this.methodName)
            child.setAttribute(HOT_RELOAD, this.enableHotReloadOnFileSaved.toString())
        }
    }

    private fun Element.setAttributeSafely(name: String, value: String?) = value?.let { this.setAttribute(name, it) }
}