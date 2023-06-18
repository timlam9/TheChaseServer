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

    private var currentQuestionPosition = 0
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
            gameQuestions.forEachIndexed { index, gameQuestion ->
                println("Question $index: $gameQuestion")
            }
        }
    }

    fun startGame(): ChaseState {
        _state = initialPlayingState()
        return _state
    }

    fun gameAnswer(gamePlayer: GameQuestionOption.SelectedBy, answer: GameQuestionOption.Position): ChaseState {
        val options: MutableList<GameQuestionOption> = _state.currentQuestion.options.toMutableList()
        val questionToChange = options.first { it.position == answer }
        val questionToAdd =
            if (questionToChange.selectedBy == GameQuestionOption.SelectedBy.NONE) {
                questionToChange.copy(selectedBy = gamePlayer)
            } else {
                questionToChange.copy(selectedBy = GameQuestionOption.SelectedBy.BOTH)
            }

        options.remove(questionToChange)
        options.add(questionToAdd)
        options.sortBy { it.position }

        _state = _state.copy(
            currentQuestion = _state.currentQuestion.copy(
                options = options
            )
        )
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

    fun showAnswer(): ChaseState {
        _state = _state.copy(
            currentQuestion = _state.currentQuestion.copy(
                showRightAnswer = true
            )
        )

        return _state
    }

    fun updateBoard(): ChaseState {
        val board = _state.board.toMutableList()
        val options = _state.currentQuestion.options

        val rightAnswer = options.first { it.isRightAnswer }.position
        val playerAnswer = options.firstOrNull { it.selectedBy == GameQuestionOption.SelectedBy.PLAYER }?.position
        val chaserAnswer = options.firstOrNull { it.selectedBy == GameQuestionOption.SelectedBy.CHASER }?.position

        println("New board 0: Right answer: $rightAnswer, Player: $playerAnswer, Chaser: $chaserAnswer")

        if (chaserAnswer == null || chaserAnswer == rightAnswer) {
            val chaserPosition = board.first { it.type == ChaseBox.RowType.CHASER_HEAD }.position
            val nextPosition = chaserPosition + 1

            board[chaserPosition] = ChaseBox(position = chaserPosition, type = ChaseBox.RowType.CHASER)
            board[nextPosition] = ChaseBox(position = nextPosition, type = ChaseBox.RowType.CHASER_HEAD)
            println("New board 1: $board")
        }

        if (playerAnswer == null || playerAnswer == rightAnswer) {
            val playerPosition = board.first { it.type == ChaseBox.RowType.PLAYER_HEAD }.position
            val nextPosition = playerPosition + 1

            board[playerPosition] = ChaseBox(position = playerPosition, type = ChaseBox.RowType.EMPTY)
            board[nextPosition] = ChaseBox(position = nextPosition, type = ChaseBox.RowType.PLAYER_HEAD)
            println("New board 2: $board")
        }

        println("New board 3: $board")
        _state = _state.copy(board = board)

        return _state
    }

    fun nextQuestion(): ChaseState {
        currentQuestionPosition += 1
        val nextQuestion = gameQuestions[currentQuestionPosition]

        _state = _state.copy(currentQuestion = nextQuestion)

        return _state
    }
}
