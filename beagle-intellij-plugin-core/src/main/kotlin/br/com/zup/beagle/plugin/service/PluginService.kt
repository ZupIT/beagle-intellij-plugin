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
import com.intellij.openapi.util.text.Strings
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.apache.commons.lang3.StringUtils
import kotlin.script.experimental.jvm.util.classPathFromTypicalResourceUrls
import java.io.File
import java.util.ArrayList

class PluginService(private val project: Project) {

    private val stopRunner = StopRunning(this.project)
    private val runnerManager = RunManager.getInstance(this.project)
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
            stopRunner.stopRunningCustomRunConfigurations()
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

//    fun getPluginClassPath() = (this.plugin as IdeaPluginDescriptorImpl).classPath.map { it.toURI().path }

    fun getClassPath(): List<String> {
        val path: File = this.plugin.pluginPath.toFile()
        if (!path.isDirectory) {
            return listOf(path).map { it.toURI().path }
        }
        val result: MutableList<File> = ArrayList()
        val classesDir = File(path, "classes")
        if (classesDir.exists()) {
            result.add(classesDir)
        }
        val files = File(path, "lib").listFiles()
        if (files == null || files.size <= 0) {
            return result.map { it.toURI().path }
        }
        for (f in files) {
            if (f.isFile) {
                val name = f.name
                if (Strings.endsWithIgnoreCase(name, ".jar") || Strings.endsWithIgnoreCase(name, ".zip")) {
                    result.add(f)
                }
            } else {
                result.add(f)
            }
        }
        return result.map { it.toURI().path }
    }
}