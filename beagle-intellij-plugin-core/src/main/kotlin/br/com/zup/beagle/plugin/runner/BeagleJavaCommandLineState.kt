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

import br.com.zup.beagle.plugin.service.JsonConverterService
import br.com.zup.beagle.plugin.service.PluginService
import br.com.zup.beagle.plugin.socket.SocketServer
import com.google.common.base.Charsets
import com.intellij.compiler.impl.ProjectCompileScope
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.compiler.CompileContext
import com.intellij.openapi.compiler.CompileStatusNotification
import com.intellij.openapi.compiler.CompilerManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.task.ProjectTaskManager
import com.intellij.task.ProjectTaskNotification


open class BeagleJavaCommandLineState(environment: ExecutionEnvironment, private val virtualFile: VirtualFile?, private val methodName: String?) : JavaCommandLineState(environment) {

    private val jsonConverterService = JsonConverterService.getInstance(this.environment.project)
    private val pluginService = PluginService.getInstance(this.environment.project)
    private var mockProcessHandler: OSProcessHandler? = null
    private var console: ConsoleView? = null

    override fun createJavaParameters(): JavaParameters {
        val params = JavaParameters()
        params.mainClass = SocketServer::class.java.name.plus("Kt")
        params.charset = Charsets.UTF_8
        params.jdk = ProjectRootManager.getInstance(this.environment.project).projectSdk
        params.classPath.addAll(this.pluginService.getPluginClassPath().toList())
        return params
    }

//    private fun getClassPathNormalized() : ArrayList<String>{
//        val classPath = ArrayList<String>()
//        this.pluginService.getPluginClassPath().toList().forEach{
//            classPath.add(it.removePrefix("/"))
//        }
//        return classPath
//    }

    override fun createConsole(executor: Executor) = super.createConsole(executor).also { this.console = it }

    override fun execute(executor: Executor, runner: ProgramRunner<*>) =
        createConsole(executor).let { console ->
            this.jsonConverterService.buildData(this.virtualFile, this.methodName, console!!).let {
                if (it != null) {
                    this.jsonConverterService.sendDataToSocket(it, console)
                    super.execute(executor, runner)
                } else {
                    object : ExecutionResult {
                        override fun getExecutionConsole() = console

                        override fun getProcessHandler() = getMockProcessHandler()

                        override fun getActions() = emptyArray<AnAction>()
                    }
                }
            }
        }

    open fun compileAndExecuteJsonConverter() {
        ProjectTaskManager.getInstance(this.environment.project).buildAllModules().onProcessed {
            if (it.isAborted.not() and it.hasErrors().not()) {
                this.jsonConverterService.buildDataAndSendToSocket(this.virtualFile, this.methodName, this.console!!)
            } else {
                this.console!!.print("\nError on building project\n", ConsoleViewContentType.ERROR_OUTPUT)
            }
        }
    }

    fun finalize() {
        this.mockProcessHandler?.destroyProcess()
    }

    private fun getMockProcessHandler() = this.javaParameters.createOSProcessHandler().also {
        this.mockProcessHandler = it
    }
}