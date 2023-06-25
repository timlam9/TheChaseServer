package com.thechase.data.webSocket

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.thechase.JWT
import com.thechase.auth.MySession
import com.thechase.data.webSocket.SocketMessage.InBound.*
import com.thechase.data.webSocket.connections.ConnectionsHandler
import com.thechase.domain.Brain
import com.thechase.domain.models.ChaseSoundEvent
import com.thechase.domain.models.GameAction
import com.thechase.domain.models.GameQuestionOption
import com.thechase.domain.models.GameStatus
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

                        val soundEvent = when (player) {
                            GameQuestionOption.SelectedBy.CHASER -> ChaseSoundEvent.CHASER_LOCK
                            GameQuestionOption.SelectedBy.PLAYER -> ChaseSoundEvent.PLAYER_LOCK
                            else -> null
                        }

                        val newChaseState = brain.gameAnswer(player, payload.position)
                        val messageToClient = SocketMessage.OutBound.State(chaseState = newChaseState)

                        soundEvent?.let { event ->
                            val messageToHost = SocketMessage.OutBound.Event(event = event)
                            sendMessageToHostConnection(connectionsHandler, messageToHost)

                            val playerNotAnswered = newChaseState.currentQuestion.options.firstOrNull {
                                it.selectedBy == GameQuestionOption.SelectedBy.PLAYER
                            } == null
                            val chaserNotAnswered = newChaseState.currentQuestion.options.firstOrNull {
                                it.selectedBy == GameQuestionOption.SelectedBy.CHASER
                            } == null
                            val bothNotAnswered = newChaseState.currentQuestion.options.firstOrNull {
                                it.selectedBy == GameQuestionOption.SelectedBy.BOTH
                            } == null

                            if ((playerNotAnswered || chaserNotAnswered) && bothNotAnswered) {
                                val countDownEvent =
                                    SocketMessage.OutBound.Event(event = ChaseSoundEvent.QUESTION_COUNTDOWN)
                                sendMessageToHostConnection(connectionsHandler, countDownEvent)
                            }
                            if (!bothNotAnswered || (!playerNotAnswered && !chaserNotAnswered)) {
                                val stopCountDownEvent =
                                    SocketMessage.OutBound.Event(event = ChaseSoundEvent.STOP_QUESTION_COUNTDOWN)
                                sendMessageToHostConnection(connectionsHandler, stopCountDownEvent)
                            }
                        }

                        sendMessageToAllConnections(connectionsHandler, messageToClient)
                    }

                    is HostAction -> {
                        val newChaseState = when (payload.action) {
                            GameAction.START -> brain.startGame(payload.questionID)
                            GameAction.SHOW_PLAYER_ANSWER -> brain.showPlayerAnswer()
                            GameAction.SHOW_RIGHT_ANSWER -> brain.showRightAnswer()
                            GameAction.SHOW_CHASER_ANSWER -> brain.showChaserAnswer()
                            GameAction.MOVE_PLAYER -> brain.movePlayer()
                            GameAction.MOVE_PLAYER_BACK -> brain.movePlayerBack()
                            GameAction.MOVE_CHASER -> brain.moveChaser()
                            GameAction.MOVE_CHASER_BACK -> brain.moveChaserBack()
                            GameAction.NEXT_QUESTION -> brain.nextQuestion()
                            GameAction.START_FINAL -> brain.startFinal(payload.timer)
                            GameAction.ADD_CHASER_FINAL_POINT -> brain.addChaserFinalPoint()
                            GameAction.ADD_PLAYER_FINAL_POINT -> brain.addPlayerFinalPoint()
                            GameAction.REMOVE_CHASER_FINAL_POINT -> brain.removeChaserFinalPoint()
                            GameAction.REMOVE_PLAYER_FINAL_POINT -> brain.removePlayerFinalPoint()
                            GameAction.PAUSE_FINAL_TIMER -> brain.pauseFinalTimer()
                            GameAction.RESUME_FINAL_TIMER -> brain.resumeFinalTimer()
                            GameAction.CHANGE_PLAYER -> brain.getCurrentState()
                            GameAction.PLAY_INTRO -> brain.getCurrentState()
                            GameAction.RESET_FINAL_TIMER -> brain.resetFinalTimer()
                        }

                        val soundEvent = when (payload.action) {
                            GameAction.START -> ChaseSoundEvent.QUESTION_APPEAR
                            GameAction.SHOW_PLAYER_ANSWER -> ChaseSoundEvent.PLAYER_ANSWER
                            GameAction.SHOW_RIGHT_ANSWER -> ChaseSoundEvent.CORRECT_ANSWER
                            GameAction.SHOW_CHASER_ANSWER -> ChaseSoundEvent.CHASER_ANSWER
                            GameAction.MOVE_PLAYER -> ChaseSoundEvent.PLAYER_MOVE
                            GameAction.MOVE_PLAYER_BACK -> null
                            GameAction.MOVE_CHASER -> ChaseSoundEvent.CHASER_MOVE
                            GameAction.MOVE_CHASER_BACK -> null
                            GameAction.NEXT_QUESTION -> ChaseSoundEvent.QUESTION_APPEAR
                            GameAction.CHANGE_PLAYER -> ChaseSoundEvent.CHANGE_PLAYER
                            GameAction.PLAY_INTRO -> ChaseSoundEvent.INTRO
                            GameAction.START_FINAL -> null
                            GameAction.ADD_CHASER_FINAL_POINT -> null
                            GameAction.ADD_PLAYER_FINAL_POINT -> null
                            GameAction.REMOVE_CHASER_FINAL_POINT -> null
                            GameAction.REMOVE_PLAYER_FINAL_POINT -> null
                            GameAction.PAUSE_FINAL_TIMER -> null
                            GameAction.RESUME_FINAL_TIMER -> null
                            GameAction.RESET_FINAL_TIMER -> null
                        }
                        soundEvent?.let { event ->
                            val messageToHost = SocketMessage.OutBound.Event(event = event)
                            sendMessageToHostConnection(connectionsHandler, messageToHost)

                            if (newChaseState.gameStatus == GameStatus.PLAYER_WIN) {
                                val playerWinEvent = SocketMessage.OutBound.Event(event = ChaseSoundEvent.PLAYER_WINS)
                                sendMessageToHostConnection(connectionsHandler, playerWinEvent)
                            } else if (newChaseState.gameStatus == GameStatus.CHASER_WIN) {
                                val chaserWinEvent = SocketMessage.OutBound.Event(event = ChaseSoundEvent.CHASER_WINS)
                                sendMessageToHostConnection(connectionsHandler, chaserWinEvent)
                            }
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
        println("- - - - - - - - - - - -  - > client email: $clientId")

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
                    println("- - - - - - - - - - - -  - > MESSAGE_RECEIVED: $payload")
                    handleFrame(this, clientId.toString(), frameTextReceived, payload)
                }
            }
        } catch (e: Exception) {
            println("- - - - - - - - - - - -  - > MESSAGE_RECEIVED ERROR: ${e.message}")
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

private suspend fun sendMessageToHostConnection(
    connectionsHandler: ConnectionsHandler,
    stateMessage: SocketMessage.OutBound
) {
    val hostConnection = connectionsHandler.connections.first { it.email == "host@gmail.com" }
    println("< - - - - - - - - - - Send to host: $hostConnection")
    val message = gson.toJson(stateMessage)
    hostConnection.session.send(Frame.Text(message))
}
