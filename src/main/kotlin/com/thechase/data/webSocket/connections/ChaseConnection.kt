package com.thechase.data.webSocket.connections

import io.ktor.server.websocket.*

data class ChaseConnection(
    val session: WebSocketServerSession,
    val email: String
)