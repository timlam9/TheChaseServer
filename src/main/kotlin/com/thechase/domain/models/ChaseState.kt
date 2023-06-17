package com.thechase.domain.models

data class ChaseState(
    val board: List<ChaseBox> = emptyList(),
    val gameStatus: GameStatus = GameStatus.SETUP,
    val currentQuestion: GameQuestion = GameQuestion(
        title = "",
        options = emptyList(),
        showRightAnswer = true,
    )
)
