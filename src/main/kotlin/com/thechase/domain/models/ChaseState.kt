package com.thechase.domain.models

data class ChaseState(
    val board: List<ChaseBox> = emptyList(),
    val gameStatus: GameStatus = GameStatus.SETUP,
    val currentQuestion: GameQuestion = GameQuestion(
        id = "",
        title = "",
        options = emptyList(),
        showRightAnswer = false,
        showPlayerAnswer = false,
        showChaserAnswer = false,
    ),
    val final: ChaseFinal = ChaseFinal()
)
