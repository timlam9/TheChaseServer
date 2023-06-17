package com.thechase.domain

import com.thechase.data.models.Position
import com.thechase.data.models.Type
import com.thechase.data.repository.QuestionRepository
import com.thechase.data.repository.Repository
import com.thechase.domain.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class Brain(val repository: Repository = QuestionRepository()) {

    private var _state = ChaseState()
    private var gameQuestions: List<GameQuestion> = emptyList()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            gameQuestions = repository.getQuestions().map {
                GameQuestion(
                    title = it.text,
                    options = it.answers.map { answer ->
                        GameQuestionOption(
                            title = answer.text,
                            position = when (answer.position) {
                                Position.A -> GameQuestionOption.Position.A
                                Position.B -> GameQuestionOption.Position.B
                                Position.C -> GameQuestionOption.Position.C
                            },
                            selectedBy = GameQuestionOption.SelectedBy.NONE,
                            isRightAnswer = when (answer.type) {
                                Type.CORRECT -> true
                                Type.WRONG -> false
                            }
                        )
                    },
                    showRightAnswer = false
                )
            }
        }
    }

    fun startGame(): ChaseState {
        _state = initialPlayingState()
        return _state
    }

    private fun initialPlayingState(): ChaseState {
        val initialList = mutableListOf(
            ChaseBox(position = 0, type = ChaseBox.RowType.CHASER_HEAD),
            ChaseBox(position = 1, type = ChaseBox.RowType.EMPTY),
            ChaseBox(position = 2, type = ChaseBox.RowType.EMPTY),
            ChaseBox(position = 3, type = ChaseBox.RowType.PLAYER_HEAD),
            ChaseBox(position = 4, type = ChaseBox.RowType.PLAYER),
            ChaseBox(position = 5, type = ChaseBox.RowType.PLAYER),
            ChaseBox(position = 6, type = ChaseBox.RowType.PLAYER),
            ChaseBox(position = 7, type = ChaseBox.RowType.PLAYER),
            ChaseBox(position = 8, type = ChaseBox.RowType.BANK),
        )

        return ChaseState(
            board = initialList,
            gameStatus = GameStatus.PLAYING,
            currentQuestion = gameQuestions.first(),
        )
    }
}
