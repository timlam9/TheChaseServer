package com.thechase

import com.thechase.plugins.*
import com.thechase.repository.DatabaseFactory
import com.thechase.repository.QuestionRepository
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.locations.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
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

@OptIn(KtorExperimentalLocationsAPI::class)
fun Application.module() {
    configureSerialization()
//    configureDatabases()
//    configureSecurity()


//    install(Sessions) {
//        cookie<MySession>("MY_SESSION") {
//            cookie.extensions["SameSite"] = "lax"
//        }
//    }

    DatabaseFactory.init()
    val db = QuestionRepository()
//    val jwtService = JwtService()
//    val hashFunction = { s: String -> hash(s) }


//    install(Authentication) {
//        jwt("jwt") {
//            verifier(jwtService.verifier)
//            realm = "The Chase Server"
//            validate {
//                val payload = it.payload
//                val claim = payload.getClaim("id")
//                val claimString = claim.asInt()
//                val user = db.findUser(claimString)
//                user
//            }
//        }
//    }

    configureRouting(db)
}
