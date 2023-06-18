package com.thechase.data.webSocket

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.thechase.JWT
import com.thechase.auth.MySession
import com.thechase.data.webSocket.SocketMessage.InBound.*
import com.thechase.data.webSocket.connections.ConnectionsHandler
import com.thechase.domain.Brain
import com.thechase.domain.models.GameAction
import com.thechase.domain.models.GameQuestionOption
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import java.util.*

private const val WEB_SOCKET_PATH = "/thechase"

private val gson: Gson = Gson()
private val brain: Brain = Brain()

fun Application.brainRouting(connectionsHandler: ConnectionsHandler) {
    routing {
        authenticate(JWT) {
            standardWebSocket { socket, clientId, _, payload ->
                when (payload) {
                    is Connect -> connectionsHandler.createConnection(socket, clientId, payload.email)
                    is Disconnect -> connectionsHandler.sendDisconnectionMessageAndDestroyGame(payload)
                    is PlayerAnswer -> {
                        val player = when (payload.email) {
                            "chaser@gmail.com" -> GameQuestionOption.SelectedBy.CHASER
                            "player@gmail.com" -> GameQuestionOption.SelectedBy.PLAYER
                            else -> GameQuestionOption.SelectedBy.NONE
                        }

                        val newChaseState = brain.gameAnswer(player, payload.position)
                        val messageToClient = SocketMessage.OutBound.State(chaseState = newChaseState)
                        sendMessageToAllConnections(connectionsHandler, messageToClient)
                    }

                    is HostAction -> {
                        val newChaseState = when (payload.action) {
                            GameAction.START -> brain.startGame()
                            GameAction.SHOW_ANSWER -> brain.showAnswer()
                            GameAction.UPDATE_BOARD -> brain.updateBoard()
                            GameAction.NEXT_QUESTION -> brain.nextQuestion()
                        }

                        val messageToClient = SocketMessage.OutBound.State(chaseState = newChaseState)
                        sendMessageToAllConnections(connectionsHandler, messageToClient)
                    }
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
        payload: SocketMessage.InBound
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
                        "player_answer" -> PlayerAnswer::class.java
                        "host_action" -> HostAction::class.java
                        else -> SocketMessage.InBound::class.java
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
