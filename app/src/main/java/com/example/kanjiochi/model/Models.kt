package com.example.kanjiochi.model

enum class QuestionType { READ, WRITE }

enum class Difficulty(val label: String, val levels: List<Int>) {
    GRADE3("3級", listOf(3)),
    GRADE2("2級", listOf(2)),
    GRADE1("1級", listOf(1)),
    MIX("ミックス", listOf(3, 2, 1)),
}

/** questions.json の1件分 */
data class Question(
    val level: Int,
    val type: QuestionType,
    val kanji: String,
    val reading: String,
    val sentence: String,
)

/** 画面上を落ちてくる爆弾1個分 */
data class Bomb(
    val id: Int,
    val question: Question,
    /** 横位置（0..1、エリア幅に対する中心位置の比率） */
    val x: Float,
    /** 落下進行度（0=上端、1=下端到達） */
    val progress: Float = 0f,
    /** 爆発演出の進行度（null=通常、0..1=爆発中） */
    val explosion: Float? = null,
) {
    val isExploding get() = explosion != null
    val isDanger get() = progress >= GameConfig.DANGER_PROGRESS
}

enum class GamePhase { START, PLAYING, GAME_OVER }

data class GameUiState(
    val phase: GamePhase = GamePhase.START,
    val difficulty: Difficulty = Difficulty.GRADE3,
    val bombs: List<Bomb> = emptyList(),
    val lives: Int = GameConfig.INITIAL_LIVES,
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    /** 書き問題で次に書く文字の位置 */
    val writeIndex: Int = 0,
    /** 画面フラッシュ（0..1） */
    val flash: Float = 0f,
    /** 直近の判定メッセージ */
    val message: String = "",
) {
    /** 一番下にある（最も危険な）爆弾。回答エリアはこれに合わせる */
    val target: Bomb? get() = bombs.filter { !it.isExploding }.maxByOrNull { it.progress }
    val multiplier: Int get() = GameConfig.multiplierFor(combo)
}
