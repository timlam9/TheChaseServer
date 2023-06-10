package com.thechase.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: Int,
    val text: String,
    val answers: List<Answer>
)

@Serializable
data class Answer(
    val position: Position,
    val text: String,
    val type: Type
)

@Serializable
enum class Position {

    @SerialName("A")
    A,
    @SerialName("B")
    B,
    @SerialName("C")
    C
}

@Serializable
enum class Type {

    @SerialName("CORRECT")
    CORRECT,
    @SerialName("WRONG")
    WRONG
}
