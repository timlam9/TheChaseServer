package com.thechase

import com.thechase.auth.JwtService
import com.thechase.auth.MySession
import com.thechase.auth.hash
import com.thechase.plugins.configureRouting
import com.thechase.plugins.configureSerialization
import com.thechase.repository.DatabaseFactory
import com.thechase.repository.QuestionRepository
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.sessions.*

const val API_VERSION = "/v1"

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

    DatabaseFactory.init()
    val db = QuestionRepository()
    val jwtService = JwtService()
    val hashFunction = { s: String -> hash(s) }

    configureSecurity(
        db = db,
        jwtService = jwtService
    )
    configureRouting(
        db = db,
        jwtService = jwtService,
        hashFunction = hashFunction
    )
}

private fun Application.configureSession() {
    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }
}

private fun Application.configureSecurity(
    db: QuestionRepository,
    jwtService: JwtService,
) {
    install(Authentication) {
        jwt("jwt") {
            verifier(jwtService.verifier)
            realm = "The Chase Server"
            validate {
                val payload = it.payload
                val claim = payload.getClaim("id")
                val claimString = claim.asInt()
                val user = db.findUser(claimString)
                user
            }
        }
    }
}
