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

import br.com.zup.beagle.plugin.util.BeagleIcons
import com.intellij.execution.configurations.ConfigurationType
import org.apache.commons.lang3.StringUtils

open class BeagleRunConfigurationType : ConfigurationType {

    open fun getRunConfigurationName(clazz: String, methodName: String) = displayName.plus(StringUtils.SPACE).plus(clazz).plus(":").plus(methodName)

    override fun getIcon() = BeagleIcons.BEAGLE_ICON

    override fun getConfigurationTypeDescription() = "Beagle Run Configuration Type"

    override fun getId() = "BEAGLE_RUN_CONFIGURATION"

    override fun getDisplayName() = "Beagle Plugin"

    override fun getConfigurationFactories() = arrayOf(BeagleConfigurationFactory(this))
}