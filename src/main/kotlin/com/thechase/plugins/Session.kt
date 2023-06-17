package com.thechase.plugins

import com.thechase.auth.MySession
import io.ktor.server.application.*
import io.ktor.server.sessions.*

fun Application.configureSession() {
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }
}

//fun Application.configureSession() {
//    install(Sessions) {
//        cookie<TicTacToeGameSession>("SESSION")
//    }
//
//    intercept(ApplicationCallPipeline.Features) {
//        if (call.sessions.get<TicTacToeGameSession>() == null) {
//            val clientId = call.parameters["clientId"] ?: ""
//            call.sessions.set(TicTacToeGameSession(clientId, generateNonce()))
//        }
//    }
//}