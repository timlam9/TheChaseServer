package com.thechase

import com.thechase.auth.JwtService
import com.thechase.auth.hash
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import com.thechase.plugins.*
import com.thechase.data.repository.QuestionRepository

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        val db = QuestionRepository()
        val jwtService = JwtService()
        val hashFunction = { s: String -> hash(s) }

        application {
            configureRouting(db, jwtService, hashFunction)
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Hello World!", bodyAsText())
        }
    }
}
