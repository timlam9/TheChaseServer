package com.thechase.models


import io.ktor.server.auth.*
import java.io.Serializable

@kotlinx.serialization.Serializable
data class User(
    val userId: Int,
    val email: String,
    val displayName: String,
    val passwordHash: String
) : Serializable, Principal