package com.thechase.data.webSocket

import com.thechase.domain.models.ChaseState

sealed class SocketMessage {

    sealed class InBound : SocketMessage() {

        data class Connect(val type: String = "connect", val email: String) : InBound()

        data class Disconnect(val type: String = "disconnect", val email: String) : InBound()

        data class Start(val type: String = "start") : InBound()
    }

    sealed class OutBound : SocketMessage() {

        data class SocketError(val type: String = "socket_error", val errorType: String) : OutBound()

        data class State(val type: String = "state", val chaseState: ChaseState) : OutBound()
    }
}
