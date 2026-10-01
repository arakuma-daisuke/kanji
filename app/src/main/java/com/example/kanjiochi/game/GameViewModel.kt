package com.example.kanjiochi.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kanjiochi.data.QuestionRepository
import com.example.kanjiochi.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class GameEvent { CORRECT, MISS, WRONG, CHAR_OK, GAME_OVER }

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = QuestionRepository(app)

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<GameEvent> = _events

    private var loop: Job? = null
    private var nextId = 0
    private var elapsed = 0f
    private var spawnTimer = 0f
    private var nextType = QuestionType.READ
    private val bags = mutableMapOf<QuestionType, ArrayDeque<Question>>()

    fun selectDifficulty(d: Difficulty) = _state.update { it.copy(difficulty = d) }

    fun start() {
        val d = _state.value.difficulty
        bags.clear()
        nextId = 0
        elapsed = 0f
        spawnTimer = GameConfig.BASE_SPAWN_INTERVAL - GameConfig.FIRST_SPAWN_DELAY
        nextType = QuestionType.READ
        _state.value = GameUiState(phase = GamePhase.PLAYING, difficulty = d)
        loop?.cancel()
        loop = viewModelScope.launch {
            val dt = GameConfig.TICK_MS / 1000f
            while (_state.value.phase == GamePhase.PLAYING) {
                delay(GameConfig.TICK_MS)
                tick(dt)
            }
        }
    }

    fun backToStart() {
        loop?.cancel()
        _state.update { it.copy(phase = GamePhase.START) }
    }

    private fun fallSpeed() =
        (GameConfig.BASE_FALL_SPEED + elapsed * GameConfig.FALL_ACCEL_PER_SEC)
            .coerceAtMost(GameConfig.MAX_FALL_SPEED)

    private fun spawnInterval() =
        (GameConfig.BASE_SPAWN_INTERVAL - elapsed * GameConfig.SPAWN_INTERVAL_DECAY_PER_SEC)
            .coerceAtLeast(GameConfig.MIN_SPAWN_INTERVAL)

    private fun nextQuestion(type: QuestionType): Question {
        val bag = bags.getOrPut(type) { ArrayDeque() }
        if (bag.isEmpty()) bag.addAll(repo.pool(_state.value.difficulty, type).shuffled())
        return bag.removeFirst()
    }

    private fun tick(dt: Float) {
        elapsed += dt
        spawnTimer += dt
        val speed = fallSpeed()
        var lives = _state.value.lives
        var combo = _state.value.combo
        var missed = false

        var bombs = _state.value.bombs.mapNotNull { b ->
            when {
                b.explosion != null -> {
                    val e = b.explosion + dt / GameConfig.EXPLOSION_SEC
                    if (e >= 1f) null else b.copy(explosion = e)
                }
                else -> {
                    val p = b.progress + speed * dt
                    if (p >= 1f) {
                        lives--; combo = 0; missed = true
                        null
                    } else b.copy(progress = p)
                }
            }
        }

        if (spawnTimer >= spawnInterval() && bombs.count { !it.isExploding } < GameConfig.MAX_BOMBS) {
            spawnTimer = 0f
            val q = nextQuestion(nextType)
            nextType = if (nextType == QuestionType.READ) QuestionType.WRITE else QuestionType.READ
            bombs = bombs + Bomb(nextId++, q, x = 0.15f + Random.nextFloat() * 0.7f)
        }

        val s = _state.value
        var flash = (s.flash - dt / GameConfig.FLASH_SEC).coerceAtLeast(0f)
        if (missed) {
            flash = 1f
            _events.tryEmit(GameEvent.MISS)
        }
        val over = lives <= 0
        // 書き問題の対象が変わったら書く位置をリセット
        val newTarget = bombs.filter { !it.isExploding }.maxByOrNull { it.progress }
        val oldTarget = s.target
        val writeIndex = if (newTarget?.id != oldTarget?.id) 0 else s.writeIndex

        _state.value = s.copy(
            bombs = bombs, lives = lives.coerceAtLeast(0), combo = combo,
            flash = flash, writeIndex = writeIndex,
            phase = if (over) GamePhase.GAME_OVER else s.phase,
        )
        if (over) _events.tryEmit(GameEvent.GAME_OVER)
    }

    private fun explode(id: Int) {
        val s = _state.value
        val gained = GameConfig.BASE_SCORE * GameConfig.multiplierFor(s.combo)
        val combo = s.combo + 1
        _state.value = s.copy(
            bombs = s.bombs.map { if (it.id == id) it.copy(explosion = 0f) else it },
            score = s.score + gained,
            combo = combo,
            maxCombo = maxOf(s.maxCombo, combo),
            writeIndex = 0,
            message = "+$gained",
        )
        _events.tryEmit(GameEvent.CORRECT)
    }

    private fun wrong() {
        _state.update { it.copy(combo = 0, message = "ちがう！") }
        _events.tryEmit(GameEvent.WRONG)
    }

    /** 読み問題の回答。画面上の読み問題のどれかに一致すれば正解 */
    fun submitReading(input: String): Boolean {
        val text = input.trim()
        if (text.isEmpty()) return false
        val hit = _state.value.bombs
            .filter { !it.isExploding && it.question.type == QuestionType.READ }
            .filter { it.question.reading == text }
            .maxByOrNull { it.progress }
        return if (hit != null) { explode(hit.id); true } else { wrong(); false }
    }

    /** 書き問題: 手書き認識の候補(上位N件)を受け取り、次の1文字が含まれるか判定 */
    fun submitWrittenChar(candidates: List<String>): Boolean {
        val s = _state.value
        val b = s.target ?: return false
        if (b.question.type != QuestionType.WRITE) return false
        val expected = b.question.kanji.getOrNull(s.writeIndex)?.toString() ?: return false
        val ok = candidates.take(GameConfig.RECOGNIZE_CANDIDATES).any { it.startsWith(expected) }
        if (!ok) { wrong(); return false }
        val next = s.writeIndex + 1
        if (next >= b.question.kanji.length) {
            explode(b.id)
        } else {
            _state.update { it.copy(writeIndex = next, message = "OK！") }
            _events.tryEmit(GameEvent.CHAR_OK)
        }
        return true
    }
}
