package com.thechase.plugins

import com.thechase.repository.QuestionRepository
import com.thechase.routes.questions
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.locations.*

@KtorExperimentalLocationsAPI
fun Application.configureRouting(db: QuestionRepository) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(text = "500: $cause" , status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respondText("The Chase")
        }

        questions(db)
    }
}
