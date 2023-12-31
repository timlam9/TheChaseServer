package com.thechase.data.repository

import com.thechase.data.models.Answer
import com.thechase.data.models.Question
import com.thechase.data.models.User
import com.thechase.data.repository.DatabaseFactory.dbQuery
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

    override suspend fun getUsers(): List<User?> {
        return transaction {
            Users.selectAll().map { rowToUser(it) }
        }
    }

    override suspend fun addQuestion(text: String, answers: List<Answer>): Question? {
        var question: com.thechase.data.repository.Question? = null

        transaction {
            question = try {
                com.thechase.data.repository.Question.new {
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
                    com.thechase.data.repository.Answer.new {
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
            com.thechase.data.repository.Question.all().map { it.toQuestion() }
        }
    }

    override suspend fun getQuestions(offset: Long, limit: Int): List<Question> {
        return com.thechase.data.repository.Question.all().limit(n = limit, offset = offset).map { it.toQuestion() }
    }

    override suspend fun deleteQuestion(questionId: Int) {
        dbQuery {
            Questions.deleteWhere {
                id.eq(questionId)
            }
        }
    }

    override suspend fun findQuestion(questionId: Int): Question? {
        return com.thechase.data.repository.Question.findById(questionId)?.toQuestion()
    }

    private fun com.thechase.data.repository.Question.toQuestion(): Question {
        val answers = com.thechase.data.repository.Answer
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
