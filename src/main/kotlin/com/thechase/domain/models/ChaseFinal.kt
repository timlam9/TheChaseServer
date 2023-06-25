package com.thechase.domain.models

data class ChaseFinal(
    val timer: Int = 120,
    val startTimer: Boolean = false,
    val pauseTimer: Boolean = false,
    val resetTimer: Int = 0,
    val playersPoints: Int = 0,
    val chaserPoints: Int = 0
)
