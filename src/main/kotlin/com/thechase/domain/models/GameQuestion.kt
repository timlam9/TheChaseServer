package com.thechase.domain.models

data class GameQuestion(
    val title: String,
    val options: List<GameQuestionOption>,
    val showRightAnswer: Boolean,
    val showPlayerAnswer: Boolean,
    val showChaserAnswer: Boolean,
)
