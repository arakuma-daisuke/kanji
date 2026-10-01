package com.example.kanjiochi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kanjiochi.game.GameViewModel
import com.example.kanjiochi.game.InkRecognizer
import com.example.kanjiochi.model.GamePhase
import com.example.kanjiochi.ui.*

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()
    private val recognizer = InkRecognizer()
    private val sound = SoundPlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recognizer.prepare()
        setContent {
            MaterialTheme {
                val state by vm.state.collectAsStateWithLifecycle()
                val modelState by recognizer.modelState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) { vm.events.collect { sound.play(it) } }

                when (state.phase) {
                    GamePhase.START -> StartScreen(
                        selected = state.difficulty,
                        modelState = modelState,
                        onSelect = vm::selectDifficulty,
                        onStart = vm::start,
                        onRetryModel = recognizer::prepare,
                    )
                    GamePhase.PLAYING -> PlayScreen(
                        state = state,
                        modelReady = modelState == InkRecognizer.ModelState.READY,
                        onSubmitReading = { vm.submitReading(it) },
                        onRecognize = { strokes, w, h ->
                            recognizer.recognize(strokes, w, h) { vm.submitWrittenChar(it) }
                        },
                    )
                    GamePhase.GAME_OVER -> GameOverScreen(state, onRetry = vm::start, onBack = vm::backToStart)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.close()
        sound.release()
    }
}
