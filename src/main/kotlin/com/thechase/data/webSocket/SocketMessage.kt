package com.thechase.data.webSocket

import com.thechase.domain.models.ChaseSoundEvent
import com.thechase.domain.models.ChaseState
import com.thechase.domain.models.GameAction
import com.thechase.domain.models.GameQuestionOption

sealed class SocketMessage {

    sealed class InBound : SocketMessage() {

        data class Connect(val type: String = "connect", val email: String) : InBound()

        data class Disconnect(val type: String = "disconnect", val email: String) : InBound()

        data class PlayerAnswer(
            val type: String = "player_answer",
            val email: String,
            val position: GameQuestionOption.Position
        ) : InBound()

        data class HostAction(
            val type: String = "host_action",
            val questionID: Int? = null,
            val timer: Int = 120,
            val action: GameAction
        ) : InBound()
    }

    sealed class OutBound : SocketMessage() {

        data class SocketError(val type: String = "socket_error", val errorType: String) : OutBound()

        data class State(val type: String = "state", val chaseState: ChaseState) : OutBound()

        data class Event(val type: String = "event", val event: ChaseSoundEvent) : OutBound()
    }
}
