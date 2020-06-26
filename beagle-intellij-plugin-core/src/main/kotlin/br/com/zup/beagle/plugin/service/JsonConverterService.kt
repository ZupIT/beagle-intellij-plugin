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

import br.com.zup.beagle.plugin.socket.SocketClient
import br.com.zup.beagle.plugin.socket.SocketServer
import br.com.zup.beagle.plugin.util.JsonConverterUtil
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

open class JsonConverterService(private val project: Project) {

    private val jsonConverterUtil = JsonConverterUtil(this.project)

    companion object {
        fun getInstance(project: Project): JsonConverterService = ServiceManager.getService(project, JsonConverterService::class.java)
    }

    open fun sendDataToSocket(data: String, console: ConsoleView) {
        thread {
            SocketHandler.run(data, console)
        }
    }

    open fun buildDataAndSendToSocket(virtualFile: VirtualFile?, methodName: String?, console: ConsoleView) {
        buildData(
            virtualFile = virtualFile,
            methodName = methodName,
            console = console
        )?.also {
            sendDataToSocket(it, console)
        }
    }

    open fun buildData(virtualFile: VirtualFile?, methodName: String?, console: ConsoleView) = this.jsonConverterUtil.getJsonFromClass(
        virtualFile = virtualFile,
        methodName = methodName,
        console = console
    )

    private object SocketHandler {

        private val logger = Logger.getInstance(SocketHandler::class.java)
        private val socketClient: SocketClient = SocketClient(SocketServer.SERVER_URI)

        fun run(data: String, console: ConsoleView, repeatOnError: Boolean = true) {
            try {
                TimeUnit.SECONDS.sleep(5)
                if (this.socketClient.isClosed) {
                    this.socketClient.reconnectBlocking()
                } else if (!this.socketClient.isOpen) {
                    this.socketClient.connectBlocking(5, TimeUnit.SECONDS)
                }
                this.socketClient.send(data)
            } catch (exception: Exception) {
                if (repeatOnError) {
                    console.print("\nTrying to send data", ConsoleViewContentType.LOG_WARNING_OUTPUT)
                    TimeUnit.SECONDS.sleep(8)
                    run(
                        data = data,
                        console = console,
                        repeatOnError = false
                    )
                } else {
                    this.logger.error("Error on connecting to server: $this.serverUrl", exception)
                    console.print("\nError on connecting to server: $this.serverUrl\n", ConsoleViewContentType.ERROR_OUTPUT)
                }
            }
        }
    }
}