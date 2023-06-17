package com.thechase.domain.models

data class GameQuestionOption(
    val title: String,
    val position: Position,
    val selectedBy: SelectedBy,
    val isRightAnswer: Boolean
) {
    enum class Position {

        A,
        B,
        C
    }

    enum class SelectedBy {

        CHASER,
        PLAYER,
        NONE
    }
}