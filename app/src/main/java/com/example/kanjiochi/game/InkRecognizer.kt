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

    /** strokes: 1画ごとの (x, y, 時刻ms) の列。候補文字列を返す（失敗時は空） */
    fun recognize(
        strokes: List<List<Triple<Float, Float, Long>>>,
        width: Float,
        height: Float,
        onResult: (List<String>) -> Unit,
    ) {
        val builder = Ink.builder()
        strokes.forEach { pts ->
            val sb = Ink.Stroke.builder()
            pts.forEach { (x, y, t) -> sb.addPoint(Ink.Point.create(x, y, t)) }
            builder.addStroke(sb.build())
        }
        val context = RecognitionContext.builder()
            .setWritingArea(WritingArea(width, height))
            .build()
        recognizer.recognize(builder.build(), context)
            .addOnSuccessListener { r -> onResult(r.candidates.map { it.text }) }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun close() = recognizer.close()
}
