package com.example.kanjiochi.data

import android.content.Context
import com.example.kanjiochi.model.Difficulty
import com.example.kanjiochi.model.Question
import com.example.kanjiochi.model.QuestionType
import org.json.JSONArray

/** assets/questions.json を読み込む */
class QuestionRepository(context: Context) {
    private val all: List<Question> = context.assets.open("questions.json")
        .bufferedReader().use { it.readText() }
        .let { JSONArray(it) }
        .let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Question(
                    level = o.getInt("level"),
                    type = if (o.getString("type") == "read") QuestionType.READ else QuestionType.WRITE,
                    kanji = o.getString("kanji"),
                    reading = o.getString("reading"),
                    sentence = o.getString("sentence"),
                )
            }
        }

    fun pool(difficulty: Difficulty, type: QuestionType): List<Question> =
        all.filter { it.type == type && it.level in difficulty.levels }
}
