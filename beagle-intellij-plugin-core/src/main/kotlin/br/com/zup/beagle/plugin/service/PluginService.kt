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

package br.com.zup.beagle.plugin.service

import br.com.zup.beagle.plugin.runner.BeagleConfigurationFactory
import br.com.zup.beagle.plugin.runner.BeagleRunConfiguration
import br.com.zup.beagle.plugin.runner.BeagleRunConfigurationType
import br.com.zup.beagle.plugin.runner.StopProcess
import br.com.zup.beagle.plugin.util.ClassPath
import br.com.zup.beagle.plugin.util.VfsChangesListener
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.lang3.StringUtils
import java.nio.file.Paths


class PluginService(private val project: Project) {

    private val runnerManager = RunManager.getInstance(this.project)
    private val vfsChangesListener = VfsChangesListener(this.project)
    private val plugin = PluginManagerCore.getPlugin(PluginId.getId(BEAGLE_PLUGIN_ID))!!

    init {
        vfsChangesListener.observerContentChange()
    }

    companion object {
        const val BEAGLE_PLUGIN_ID = "br.com.zup.beagle-intellij-plugin"

        fun getInstance(project: Project): PluginService = ServiceManager.getService(project, PluginService::class.java)
    }

    fun runPlugin(clazz: VirtualFile?, methodName: String?) {
        if (clazz != null && StringUtils.isNotBlank(methodName)) {
            StopProcess.stopRunningCustomRunConfigurations(project)
            val customRunConfigurationType = BeagleRunConfigurationType()
            val configurationName = customRunConfigurationType.getRunConfigurationName(clazz.name, methodName!!)
            var configurationAndSettings = this.runnerManager.findConfigurationByName(configurationName)
            if (configurationAndSettings == null) {
                val customConfigurationFactory = BeagleConfigurationFactory(customRunConfigurationType)
                val customConfiguration = BeagleRunConfiguration(
                    this.project,
                    customConfigurationFactory,
                    configurationName
                )
                customConfiguration.clazzToRunPlugin = clazz
                customConfiguration.methodName = methodName
                configurationAndSettings = this.runnerManager.createConfiguration(customConfiguration, customConfigurationFactory)
                this.runnerManager.addConfiguration(configurationAndSettings)
            } else {
                (configurationAndSettings.configuration as BeagleRunConfiguration).clazzToRunPlugin = clazz
                (configurationAndSettings.configuration as BeagleRunConfiguration).methodName = methodName
            }
            this.runnerManager.selectedConfiguration = configurationAndSettings
            ProgramRunnerUtil.executeConfiguration(configurationAndSettings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }

    fun getPluginClassPath() = ClassPath.getClassPath(this.plugin).map { Paths.get(it.toURI()).toAbsolutePath().toString() }
}