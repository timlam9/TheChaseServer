package com.thechase.repository

import com.thechase.models.Answer
import com.thechase.models.Question
import com.thechase.models.User


interface Repository {

    suspend fun addUser(
        email: String,
        displayName: String,
        passwordHash: String
    ): User?

    suspend fun deleteUser(userId: Int)
    suspend fun findUser(userId: Int): User?
    suspend fun findUserByEmail(email: String): User?

    suspend fun addQuestion(text: String, answers: List<Answer>): Question?
    suspend fun deleteQuestion(questionId: Int)
    suspend fun findQuestion(questionId: Int): Question?
    suspend fun getQuestions(): List<Question>
    suspend fun getQuestions(offset: Long, limit: Int = 100): List<Question>
}