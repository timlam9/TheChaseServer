package com.thechase.data.repository

import com.thechase.data.models.Position
import com.thechase.data.models.Type
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

object Questions : IntIdTable() {
    val text: Column<String> = varchar("text", 500)
}

class Question(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Question>(Questions)

    var text by Questions.text
}


object Answers : IntIdTable() {
    val text: Column<String> = varchar("text", 80)
    val type: Column<Type> = enumeration("type", Type::class)
    val position: Column<Position> = enumeration("position", Position::class)
    val questionID = integer("QUESTIONS_ID").references(Questions.id)
}

class Answer(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Answer>(Answers)

    var text by Answers.text
    var type by Answers.type
    var position by Answers.position
    var questionID by Answers.questionID
}
