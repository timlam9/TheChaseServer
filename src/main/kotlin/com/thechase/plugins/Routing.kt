package com.thechase.plugins

import com.thechase.auth.JwtService
import com.thechase.repository.Repository
import com.thechase.routes.questions
import com.thechase.routes.users
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    db: Repository,
    jwtService: JwtService,
    hashFunction: (String) -> String
) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respondText("The Chase Server")
        }

        users(db, jwtService, hashFunction)
        questions(db)
    }
}
