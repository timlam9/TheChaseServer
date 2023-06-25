package com.thechase.domain.models

enum class ChaseSoundEvent {

    INTRO,
    PRE_GAME, // not used yet
    QUESTION, // it is handled by the client when the question appear event is sent
    QUESTION_COUNTDOWN,
    STOP_QUESTION_COUNTDOWN,
    CHASER_WINS,
    PLAYER_WINS,
    CHANGE_PLAYER,
    CHASER_ANSWER,
    CHASER_LOCK,
    CHASER_MOVE,
    PLAYER_ANSWER,
    PLAYER_LOCK,
    PLAYER_MOVE,
    QUESTION_APPEAR,
    CORRECT_ANSWER
}
