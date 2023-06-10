package com.thechase.repository

import com.thechase.models.Answer
import com.thechase.models.Question
import com.thechase.models.User
import com.thechase.repository.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction

class QuestionRepository : Repository {

    override suspend fun addUser(email: String, displayName: String, passwordHash: String): User? {
        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = Users.insert {
                it[Users.email] = email
                it[Users.displayName] = displayName
                it[Users.passwordHash] = passwordHash
            }
        }
        return rowToUser(statement?.resultedValues?.first())
    }

    override suspend fun deleteUser(userId: Int) {
        dbQuery {
            Users.deleteWhere {
                Users.userId.eq(userId)
            }
        }
    }

    override suspend fun findUser(userId: Int) = dbQuery {
        Users.select { Users.userId.eq(userId) }
            .map { rowToUser(it) }.singleOrNull()
    }

    override suspend fun findUserByEmail(email: String) = dbQuery {
        Users.select { Users.email.eq(email) }
            .map { rowToUser(it) }.singleOrNull()
    }

    override suspend fun addQuestion(text: String, answers: List<Answer>): Question? {
        var question: com.thechase.repository.Question? = null

        transaction {
            question = try {
                com.thechase.repository.Question.new {
                    this.text = text
                }
            } catch (e: Exception) {
                println("Create repo question ========================> ${e.message}")
                null
            }

        }

        if (question == null) return null

        transaction {
            answers.forEach { answer ->
                try {
                    com.thechase.repository.Answer.new {
                        this.text = answer.text
                        this.type = answer.type
                        this.position = answer.position
                        this.questionID = question!!.id.value
                    }
                } catch (e: Exception) {
                    println("Create repo Answer ========================> ${e.message}")
                }
            }
        }

        return Question(
            id = question!!.id.value,
            text = question!!.text,
            answers = answers
        )
    }

    override suspend fun getQuestions(): List<Question> {
        return transaction {
            com.thechase.repository.Question.all().map { it.toQuestion() }
        }
    }

    override suspend fun getQuestions(offset: Long, limit: Int): List<Question> {
        return com.thechase.repository.Question.all().limit(n = limit, offset = offset).map { it.toQuestion() }
    }

    override suspend fun deleteQuestion(questionId: Int) {
        dbQuery {
            Questions.deleteWhere {
                id.eq(questionId)
            }
        }
    }

    override suspend fun findQuestion(questionId: Int): Question? {
        return com.thechase.repository.Question.findById(questionId)?.toQuestion()
    }

    private fun com.thechase.repository.Question.toQuestion(): Question {
        val answers = com.thechase.repository.Answer
            .find {
                Answers.questionID eq id.value
            }
            .limit(3)
            .map { repoAnswer ->
                Answer(
                    position = repoAnswer.position,
                    text = repoAnswer.text,
                    type = repoAnswer.type
                )
            }

        return Question(
            id = id.value,
            text = text,
            answers = answers
        )
    }

    private fun rowToUser(row: ResultRow?): User? {
        if (row == null) {
            return null
        }
        return User(
            userId = row[Users.userId],
            email = row[Users.email],
            displayName = row[Users.displayName],
            passwordHash = row[Users.passwordHash]
        )
    }
}
