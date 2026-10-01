package com.example.kanjiochi.ui

import android.media.AudioManager
import android.media.ToneGenerator
import com.example.kanjiochi.game.GameEvent

/** 簡易効果音（ToneGeneratorで鳴らすので音声ファイル不要） */
class SoundPlayer {
    private val tone = try { ToneGenerator(AudioManager.STREAM_MUSIC, 70) } catch (e: Exception) { null }

    fun play(event: GameEvent) {
        val (type, ms) = when (event) {
            GameEvent.CORRECT -> ToneGenerator.TONE_PROP_BEEP2 to 200
            GameEvent.CHAR_OK -> ToneGenerator.TONE_PROP_ACK to 120
            GameEvent.WRONG -> ToneGenerator.TONE_PROP_NACK to 150
            GameEvent.MISS -> ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE to 300
            GameEvent.GAME_OVER -> ToneGenerator.TONE_CDMA_ABBR_ALERT to 600
        }
        tone?.startTone(type, ms)
    }

    fun release() = tone?.release()
}
