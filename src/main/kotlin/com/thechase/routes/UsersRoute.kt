package com.thechase.routes

import com.thechase.API_VERSION
import com.thechase.auth.MySession
import com.thechase.repository.Repository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import com.thechase.auth.JwtService

const val USERS = "$API_VERSION/users"
const val USER_LOGIN = "$USERS/login"
const val USER_LOGOUT = "$USERS/logout"
const val USER_CREATE = "$USERS/create"
const val USER_DELETE = "$USERS/delete"

fun Route.users(
    db: Repository,
    jwtService: JwtService,
    hashFunction: (String) -> String
) {
    getUsers(db)
    login(hashFunction, db, jwtService)
    logout(db)
    delete(db)
    create(hashFunction, db, jwtService)
}

private fun Route.getUsers(db: Repository) {
    get(USERS) {
        try {
            val questions = db.getUsers()
            call.respond(questions)
        } catch (e: Throwable) {
            application.log.error("Failed to get users", e)
            call.respond(HttpStatusCode.BadRequest, "Problems getting users")
        }
    }
}

private fun Route.login(
    hashFunction: (String) -> String,
    db: Repository,
    jwtService: JwtService
) {
    post(USER_LOGIN) {
        val signinParameters = call.receive<Parameters>()
        val password =
            signinParameters["password"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")
        val email = signinParameters["email"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")

        val hash = hashFunction(password)

        try {
            val currentUser = db.findUserByEmail(email)
            currentUser?.userId?.let {
                if (currentUser.passwordHash == hash) {
                    call.sessions.set(MySession(it))
                    call.respondText(jwtService.generateToken(currentUser))
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
                }
            }
        } catch (e: Throwable) {
            application.log.error("Failed to register user", e)
            call.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
        }
    }
}

private fun Route.logout(db: Repository) {
    post(USER_LOGOUT) {
        val signinParameters = call.receive<Parameters>()
        val email = signinParameters["email"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")

        try {
            val currentUser = db.findUserByEmail(email)
            currentUser?.userId?.let {
                call.sessions.clear(call.sessions.findName(MySession::class))
                call.respond(HttpStatusCode.OK)
            }
        } catch (e: Throwable) {
            application.log.error("Failed to register user", e)
            call.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
        }
    }
}

private fun Route.delete(db: Repository) {
    delete(USER_DELETE) {
        val signinParameters = call.receive<Parameters>()
        val email =
            signinParameters["email"] ?: return@delete call.respond(HttpStatusCode.Unauthorized, "Missing Fields")

        try {
            val currentUser = db.findUserByEmail(email)
            currentUser?.userId?.let {
                db.deleteUser(it)
                call.sessions.clear(call.sessions.findName(MySession::class))
                call.respond(HttpStatusCode.OK)
            }
        } catch (e: Throwable) {
            application.log.error("Failed to register user", e)
            call.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
        }
    }
}

private fun Route.create(
    hashFunction: (String) -> String,
    db: Repository,
    jwtService: JwtService
) {
    post(USER_CREATE) {
        val signupParameters = call.receive<Parameters>()
        val password =
            signupParameters["password"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")
        val displayName =
            signupParameters["displayName"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")
        val email = signupParameters["email"] ?: return@post call.respond(HttpStatusCode.Unauthorized, "Missing Fields")

        val hash = hashFunction(password)

        try {
            val newUser = db.addUser(email, displayName, hash)
            newUser?.userId?.let {
                call.sessions.set(MySession(it))
                call.respondText(jwtService.generateToken(newUser), status = HttpStatusCode.Created)
            }
        } catch (e: Throwable) {
            application.log.error("Failed to register user", e)
            call.respond(HttpStatusCode.BadRequest, "Problems creating User")
        }
    }
}
