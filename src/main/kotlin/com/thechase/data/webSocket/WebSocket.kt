package com.thechase.data.webSocket

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.thechase.JWT
import com.thechase.auth.MySession
import com.thechase.data.webSocket.SocketMessage.InBound.*
import com.thechase.data.webSocket.connections.ConnectionsHandler
import com.thechase.domain.models.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import java.util.*

private const val WEB_SOCKET_PATH = "/thechase"

private var state = ChaseState()
private val gson: Gson = Gson()

fun Application.brainRouting(connectionsHandler: ConnectionsHandler) {
    routing {
        authenticate(JWT) {
            standardWebSocket { socket, clientId, _, payload ->
                when (payload) {
                    is Connect -> connectionsHandler.createConnection(socket, clientId, payload.email)
                    is Disconnect -> connectionsHandler.sendDisconnectionMessageAndDestroyGame(payload)
                    is Start -> {
                        val newState: ChaseState = updateToInitialPlayingState()
                        val stateMessage: SocketMessage.OutBound.State =
                            SocketMessage.OutBound.State(chaseState = newState)
                        println("< - - - - - - - - - - Message to send: $stateMessage")

                        sendMessageToAllConnections(connectionsHandler, stateMessage)
                    }

                    is SocketMessage.OutBound.SocketError -> TODO()
                    is SocketMessage.OutBound.State -> TODO()
                }
            }
        }
    }
}

fun Route.standardWebSocket(
    handleFrame: suspend (
        socket: DefaultWebSocketServerSession,
        clientId: String,
        frameTextReceived: String,
        payload: SocketMessage
    ) -> Unit
) {
    webSocket(WEB_SOCKET_PATH) {
        val chaseConnection = call.sessions.get<MySession>()
        val clientId = chaseConnection?.userId ?: UUID.randomUUID().toString()
//        kotlin.run {
//            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "clientEmail is null"))
//            return@webSocket
//        }
        println("clientEmail: $clientId")

        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    val frameTextReceived = frame.readText()
                    val jsonObject = JsonParser.parseString(frameTextReceived).asJsonObject
                    println("- - - - - - - - - - > Message received: $jsonObject")

                    val type = when (jsonObject.get("type").asString) {
                        "connect" -> Connect::class.java
                        "start" -> Start::class.java
                        else -> SocketMessage::class.java
                    }
                    val payload = gson.fromJson(frameTextReceived, type)
                    handleFrame(this, clientId.toString(), frameTextReceived, payload)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // Handle Socket Closed
        }
    }
}

private fun updateToInitialPlayingState(): ChaseState {
    val initialList = mutableListOf(
        ChaseBox(position = 0, type = ChaseBox.RowType.CHASER_HEAD),
        ChaseBox(position = 1, type = ChaseBox.RowType.EMPTY),
        ChaseBox(position = 2, type = ChaseBox.RowType.EMPTY),
        ChaseBox(position = 3, type = ChaseBox.RowType.PLAYER_HEAD),
        ChaseBox(position = 4, type = ChaseBox.RowType.PLAYER),
        ChaseBox(position = 5, type = ChaseBox.RowType.PLAYER),
        ChaseBox(position = 6, type = ChaseBox.RowType.PLAYER),
        ChaseBox(position = 7, type = ChaseBox.RowType.PLAYER),
        ChaseBox(position = 8, type = ChaseBox.RowType.BANK),
    )

    val gameQuestion = GameQuestion(
        title = "Πόσους λάκους έχει η φάβα μέσα σε μία φασολάδα με κομένη και χαροκαμένη στραπατσάδα;",
        options = listOf(
            GameQuestionOption(
                title = "Η φάβα έχει 5 λάκκους",
                position = GameQuestionOption.Position.A,
                selectedBy = GameQuestionOption.SelectedBy.NONE,
                isRightAnswer = false
            ),
            GameQuestionOption(
                title = "Η φάβα έχει 50 λάκκους",
                position = GameQuestionOption.Position.B,
                selectedBy = GameQuestionOption.SelectedBy.PLAYER,
                isRightAnswer = true
            ),
            GameQuestionOption(
                title = "Η φάβα έχει 500 λάκκους",
                position = GameQuestionOption.Position.C,
                selectedBy = GameQuestionOption.SelectedBy.CHASER,
                isRightAnswer = false
            )
        ),
        showRightAnswer = true,
    )

    val newState = ChaseState(
        board = initialList,
        gameStatus = GameStatus.PLAYING,
        currentQuestion = gameQuestion,
    )

    println("New State: $newState")
    return newState
}

private suspend fun sendMessageToAllConnections(
    connectionsHandler: ConnectionsHandler,
    stateMessage: SocketMessage.OutBound
) {
    connectionsHandler.connections.forEach {
        println("< - - - - - - - - - - Send to connection: $it")
        val message = gson.toJson(stateMessage)
        it.session.send(Frame.Text(message))
    }
}
