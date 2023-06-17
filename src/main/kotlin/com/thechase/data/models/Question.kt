package com.thechase.data.models

data class Question(
    val id: Int,
    val text: String,
    val answers: List<Answer>
)

data class Answer(
    val position: Position,
    val text: String,
    val type: Type
)

enum class Position {

    A,
    B,
    C
}

enum class Type {

    CORRECT,
    WRONG
}
