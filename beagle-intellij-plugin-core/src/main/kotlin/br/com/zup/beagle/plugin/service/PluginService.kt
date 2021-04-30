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
import br.com.zup.beagle.plugin.util.CustomVirtualFileListener
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.apache.commons.lang3.StringUtils
import kotlin.script.experimental.jvm.util.classPathFromTypicalResourceUrls

class PluginService(private val project: Project) {

    private val runnerManager = RunManager.getInstance(this.project)
    private val executionManager = ExecutionManagerImpl.getInstance(this.project)
    private val virtualFileManager = VirtualFileManager.getInstance()
    private val plugin = PluginManager.getPlugin(PluginId.getId(BEAGLE_PLUGIN_ID))!!

    init {
        this.virtualFileManager.addVirtualFileListener(CustomVirtualFileListener(this.project))
    }

    companion object {
        const val BEAGLE_PLUGIN_ID = "br.com.zup.beagle-intellij-plugin"

        fun getInstance(project: Project): PluginService = ServiceManager.getService(project, PluginService::class.java)
    }

    fun runPlugin(clazz: VirtualFile?, methodName: String?) {
        if (clazz != null && StringUtils.isNotBlank(methodName)) {
            stopRunningCustomRunConfigurations()
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

    fun getPluginClassPath() = (this.plugin as IdeaPluginDescriptorImpl).pluginClassLoader.classPathFromTypicalResourceUrls().map { it.toURI().path }

    fun stopRunningCustomRunConfigurations() {
        val descriptors = this.executionManager.getDescriptors {
            it.configuration is BeagleRunConfiguration
        }
        descriptors.forEach {
            if (!it.processHandler!!.isProcessTerminated) {
                it.processHandler!!.destroyProcess()
            }
        }
    }
}