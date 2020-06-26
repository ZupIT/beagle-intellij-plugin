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

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


open class SocketServer(
    address: InetSocketAddress?
) : WebSocketServer(address) {

    companion object {
        const val SHUTDOWN_COMMAND = "SHUTDOWN"
        const val BEAGLE_WEB_SOCKET_PORT = 9721
        val SERVER_URI = URI("ws://localhost:$BEAGLE_WEB_SOCKET_PORT")
        fun getInstance(): SocketServer = SocketServer(
            InetSocketAddress("0.0.0.0", BEAGLE_WEB_SOCKET_PORT)
        )
    }

    private var lastMessage: String? = null

    init {
        connectionLostTimeout = 0
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake?) {
        if (!this.lastMessage.isNullOrBlank()) {
            broadcast(this.lastMessage)
        } else {
            broadcast("Welcome ${conn.remoteSocketAddress}")
        }
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) = throw UnsupportedOperationException("WebSocket closed")

    override fun onMessage(conn: WebSocket, message: String) {
        treatCommandIfExists(message)
        while (this.connections.none { it.remoteSocketAddress.toString() != conn.remoteSocketAddress.toString() }) {
            println("Awaiting client connections ...")
            TimeUnit.SECONDS.sleep(5)
        }
        broadcast(message)
        this.lastMessage = message
        println("Data already sent !")
    }

    override fun onStart() = println("WebSocket server started successfully")

    override fun onError(conn: WebSocket?, ex: Exception) {
        if (ex is UnsupportedOperationException) {
            println("Disconnected client: ${conn?.remoteSocketAddress}")
        } else {
            println(ex)
        }
    }

    private fun treatCommandIfExists(command: String) {
        when (command) {
            SHUTDOWN_COMMAND -> {
                println("Server stopping, only one server can be up at time ...")
                exitProcess(0)
            }
        }
    }
}

fun main() {
    println("Initializing socket on port ${SocketServer.BEAGLE_WEB_SOCKET_PORT} ...")
    shutdownServers()
    SocketServer.getInstance().run()
}

fun shutdownServers() {
    try {
        val client = SocketClient(SocketServer.SERVER_URI)
        client.connectBlocking()
        client.send(SocketServer.SHUTDOWN_COMMAND)
        TimeUnit.SECONDS.sleep(3)
    } catch (ex: Exception) {
        //do nothing
    }
}