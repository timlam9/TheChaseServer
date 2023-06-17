package com.thechase.routes

import com.thechase.API_VERSION
import com.thechase.JWT
import com.thechase.data.models.Question
import com.thechase.data.repository.Repository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

const val QUESTIONS = "$API_VERSION/questions"

fun Route.questions(db: Repository) {
    authenticate(JWT) {
        postQuestion(db)
        getQuestions(db)
        deleteQuestion(db)
    }
}

private fun Route.getQuestions(db: Repository) {
    get(QUESTIONS) {
        val questionsParameters = call.request.queryParameters
        val limit = if (questionsParameters.contains("limit")) questionsParameters["limit"] else null
        val offset = if (questionsParameters.contains("offset")) questionsParameters["offset"] else null

        try {
            if (limit != null && offset != null) {
                val questions = db.getQuestions(offset.toLong(), limit.toInt())
                call.respond(questions)

            } else {
                val questions = db.getQuestions()
                call.respond(questions)
            }
        } catch (e: Throwable) {
            application.log.error("Failed to get questions", e)
            call.respond(HttpStatusCode.BadRequest, "Problems getting questions")
        }
    }
}

private fun Route.postQuestion(db: Repository) {
    post(QUESTIONS) {
        val question = try {
            call.receive<Question>()
        } catch (e: Exception) {
            println(e.message)
            call.respond(HttpStatusCode.BadRequest, "Crap: ${e.message}")
            null
        } ?: return@post

        try {
            val currentQuestion = db.addQuestion(question.text, question.answers)
            currentQuestion?.id?.let {
                call.respond(HttpStatusCode.OK, currentQuestion)
            }
        } catch (e: Throwable) {
            application.log.error("Failed to add question", e)
            call.respond(HttpStatusCode.BadRequest, "Problems Saving Question, $question")
        }
    }
}

private fun Route.deleteQuestion(db: Repository) {
    delete(QUESTIONS) {
        val id = call.request.queryParameters["id"]
            ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing Question Id")

        try {
            db.deleteQuestion(id.toInt())
            call.respond(HttpStatusCode.OK)
        } catch (e: Throwable) {
            application.log.error("Failed to delete question", e)
            call.respond(HttpStatusCode.BadRequest, "Problems Deleting question")
        }
    }
}
