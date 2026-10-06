package com.example.kanjiochi.game

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionContext
import com.google.mlkit.vision.digitalink.WritingArea
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** ML Kit Digital Ink Recognition（日本語）のラッパー */
class InkRecognizer {
    enum class ModelState { LOADING, READY, FAILED }

    private val _modelState = MutableStateFlow(ModelState.LOADING)
    val modelState: StateFlow<ModelState> = _modelState

    private val model = DigitalInkRecognitionModel.builder(
        DigitalInkRecognitionModelIdentifier.fromLanguageTag("ja")!!
    ).build()
    private val recognizer: DigitalInkRecognizer = DigitalInkRecognition.getClient(
        DigitalInkRecognizerOptions.builder(model).build()
    )

    /** 初回のみモデルをダウンロードする */
    fun prepare() {
        _modelState.value = ModelState.LOADING
        RemoteModelManager.getInstance()
            .download(model, DownloadConditions.Builder().build())
            .addOnSuccessListener { _modelState.value = ModelState.READY }
            .addOnFailureListener { _modelState.value = ModelState.FAILED }
    }

    /**
     * strokes: 1画ごとの (x, y, 時刻ms) の列。候補文字列を返す（失敗時は空）。
     * ML Kit は時刻が増加し続けることを前提にするので、全体で単調増加に補正する。
     */
    fun recognize(
        strokes: List<List<Triple<Float, Float, Long>>>,
        width: Float,
        height: Float,
        onResult: (List<String>) -> Unit,
    ) {
        try {
            var lastT = -1L
            val builder = Ink.builder()
            strokes.filter { it.isNotEmpty() }.forEach { pts ->
                val sb = Ink.Stroke.builder()
                pts.forEach { (x, y, t) ->
                    lastT = maxOf(t, lastT + 1)
                    sb.addPoint(Ink.Point.create(x, y, lastT))
                }
                builder.addStroke(sb.build())
            }
            val contextBuilder = RecognitionContext.builder().setPreContext("")
            if (width > 0f && height > 0f) contextBuilder.setWritingArea(WritingArea(width, height))
            recognizer.recognize(builder.build(), contextBuilder.build())
                .addOnSuccessListener { r ->
                    onResult(runCatching { r.candidates.map { it.text } }.getOrDefault(emptyList()))
                }
                .addOnFailureListener { e ->
                    CrashLog.record("認識に失敗", e)
                    onResult(emptyList())
                }
        } catch (e: Throwable) {
            CrashLog.record("認識の準備で例外", e)
            onResult(emptyList())
        }
    }

    fun close() = recognizer.close()
}
