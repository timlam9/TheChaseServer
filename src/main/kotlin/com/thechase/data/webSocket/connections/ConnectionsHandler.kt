package com.thechase.data.webSocket.connections

import com.google.gson.Gson
import com.thechase.data.webSocket.SocketMessage
import io.ktor.server.application.*
import io.ktor.server.websocket.*
import java.util.*
import io.ktor.websocket.*

private val gson = Gson()
class ConnectionsHandler(private val application: Application) {
    val connections = Collections.synchronizedSet<ChaseConnection?>(LinkedHashSet())

    fun createConnection(serverSession: DefaultWebSocketServerSession, id: String, email: String) {
        val thisConnection = ChaseConnection(serverSession, email)
        connections += thisConnection

        application.log.info(" ")
        application.log.info("------ Connections: ${connections.size}")
        application.log.info(" ")
    }

    suspend fun sendDisconnectionMessageAndDestroyGame(message: SocketMessage.InBound.Disconnect) {
        connections.removeIf { it.email == message.email }
        val error = SocketMessage.OutBound.SocketError(errorType = "ConnectionLost")

        connections.forEach {
            it.session.send(Frame.Text(gson.toJson(error)))
        }
    }
}
