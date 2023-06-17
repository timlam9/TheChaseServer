package com.thechase.plugins

import com.thechase.auth.JwtService
import com.thechase.data.repository.QuestionRepository
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity(
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