package com.example.kanjiochi.model

/** ゲームバランス用の定数 */
object GameConfig {
    const val INITIAL_LIVES = 3
    const val TICK_MS = 16L

    /** 落下速度（エリア高さに対する割合/秒）。時間経過で加速 */
    const val BASE_FALL_SPEED = 0.07f
    const val FALL_ACCEL_PER_SEC = 0.0016f
    const val MAX_FALL_SPEED = 0.22f

    /** 出現間隔（秒）。時間経過で短くなる */
    const val BASE_SPAWN_INTERVAL = 5.0f
    const val MIN_SPAWN_INTERVAL = 2.4f
    const val SPAWN_INTERVAL_DECAY_PER_SEC = 0.02f
    const val FIRST_SPAWN_DELAY = 0.5f
    const val MAX_BOMBS = 4

    /** 危険状態になる進行度（残り約25%） */
    const val DANGER_PROGRESS = 0.75f

    const val BASE_SCORE = 100
    const val COMBO_STEP = 3
    const val MAX_MULTIPLIER = 5

    const val EXPLOSION_SEC = 0.4f
    const val FLASH_SEC = 0.35f

    /** 手書き認識の候補数（上位N件に正解があれば正解） */
    const val RECOGNIZE_CANDIDATES = 5

    fun multiplierFor(combo: Int) = (1 + combo / COMBO_STEP).coerceAtMost(MAX_MULTIPLIER)
}
