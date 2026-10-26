package com.neocode.surfx

import android.content.Context
import org.json.JSONObject

data class QuizOption(val id: String, val text: String)
data class QuizQuestion(
    val id: String,
    val order: Int,
    val difficulty: String,
    val type: String,
    val topicId: String,
    val topic: String,
    val question: String,
    val options: List<QuizOption>,
    val correctOptionId: String,
    val explanation: String,
    val learningObjective: String,
    val referenceIds: List<String>
)

class QuizRepository(private val context: Context) {
    private val cache = mutableMapOf<String, List<QuizQuestion>>()

    fun questions(language: String): List<QuizQuestion> = synchronized(cache) {
        cache.getOrPut(language) {
            val json = context.assets.open("quiz/blockchain_quiz_$language.json")
                .bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            val rows = json.getJSONArray("questions")
            List(rows.length()) { i ->
                val row = rows.getJSONObject(i)
                val options = row.getJSONArray("options")
                val references = row.getJSONArray("referenceIds")
                QuizQuestion(
                    id = row.getString("id"), order = row.getInt("order"),
                    difficulty = row.getString("difficulty"), type = row.getString("type"),
                    topicId = row.getString("topicId"), topic = row.getString("topic"),
                    question = row.getString("question"),
                    options = List(options.length()) { j ->
                        val option = options.getJSONObject(j)
                        QuizOption(option.getString("id"), option.getString("text"))
                    },
                    correctOptionId = row.getString("correctOptionId"),
                    explanation = row.getString("explanation"),
                    learningObjective = row.getString("learningObjective"),
                    referenceIds = List(references.length()) { j -> references.getString(j) }
                )
            }.sortedBy { it.order }
        }
    }
}
