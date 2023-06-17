package com.thechase

import com.thechase.auth.JwtService
import com.thechase.auth.hash
import com.thechase.data.webSocket.connections.ConnectionsHandler
import com.thechase.data.webSocket.brainRouting
import com.thechase.data.repository.DatabaseFactory
import com.thechase.data.repository.QuestionRepository
import com.thechase.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

internal const val API_VERSION = "/v1"
internal const val JWT = "jwt"

fun main() {
    embeddedServer(
        Netty,
        port = 8080,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureSession()
    configureWebSockets()

    DatabaseFactory.init()
    val db = QuestionRepository()
    val jwtService = JwtService()
    val hashFunction = { s: String -> hash(s) }
    val connectionsHandler = ConnectionsHandler(this)

    configureSecurity(
        db = db,
        jwtService = jwtService
    )
    configureRouting(
        db = db,
        jwtService = jwtService,
        hashFunction = hashFunction
    )
    brainRouting(connectionsHandler)
}
