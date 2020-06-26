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

package br.com.zup.beagle.plugin.util

import br.com.zup.beagle.plugin.runner.BeagleRunConfiguration
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener

class CustomVirtualFileListener(private val project: Project) : VirtualFileListener {

    private val executionManager = ExecutionManagerImpl.getInstance(this.project)

    override fun contentsChanged(event: VirtualFileEvent) {
        if (event.isFromSave && JsonConverterUtil.AVAILABLE_EXTENSIONS.contains(event.file.extension)) {
            val runConfigurationMap = HashMap<VirtualFile, BeagleRunConfiguration>()
            val descriptors = this.executionManager.getDescriptors { filterRunningConfigurationsAndSetRunConfigurationToRunConfigurationsMap(it, runConfigurationMap) }
            if (runConfigurationMap.containsKey(event.file) && checkIfDescriptorIsRunning(descriptors)) {
                val customRunConfiguration = (runConfigurationMap[event.file] as BeagleRunConfiguration)
                customRunConfiguration.javaCommandLineState!!.compileAndExecuteJsonConverter()
            }
        }
        super.contentsChanged(event)
    }

    private fun checkIfDescriptorIsRunning(descriptors: List<RunContentDescriptor>) = descriptors.isNotEmpty() && !descriptors[0].processHandler!!.isProcessTerminated

    private fun filterRunningConfigurationsAndSetRunConfigurationToRunConfigurationsMap(runConfigurationSettings: RunnerAndConfigurationSettings, runConfigurationMap: HashMap<VirtualFile, BeagleRunConfiguration>): Boolean {
        if (runConfigurationSettings.configuration is BeagleRunConfiguration
                && (runConfigurationSettings.configuration as BeagleRunConfiguration).enableHotReloadOnFileSaved) {
            val customRunConfiguration = (runConfigurationSettings.configuration as BeagleRunConfiguration)
            runConfigurationMap[customRunConfiguration.clazzToRunPlugin!!] = customRunConfiguration
            return true
        }
        return false
    }
}