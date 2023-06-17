package com.thechase.domain.models

data class ChaseBox(
    val position: Int,
    val type: RowType,
) {

    enum class RowType(val title: String) {

        CHASER(""),
        CHASER_HEAD("Chaser"),
        PLAYER(""),
        PLAYER_HEAD("Player"),
        EMPTY(""),
        BANK("")
    }
}
