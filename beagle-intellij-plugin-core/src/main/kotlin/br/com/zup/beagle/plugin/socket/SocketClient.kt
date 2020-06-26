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

package br.com.zup.beagle.plugin.socket

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

open class SocketClient(serverUri: URI) : WebSocketClient(serverUri) {

    init {
        connectionLostTimeout = 0
    }

    override fun onOpen(handshakedata: ServerHandshake?) = Unit

    override fun onClose(code: Int, reason: String?, remote: Boolean) = Unit

    override fun onMessage(message: String?) = Unit

    override fun onError(exception: Exception?) = Unit
}