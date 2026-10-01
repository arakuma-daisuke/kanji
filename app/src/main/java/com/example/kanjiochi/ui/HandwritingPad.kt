package com.example.kanjiochi.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

typealias InkPoint = Triple<Float, Float, Long>

/** 1文字ずつ書く手書きキャンバス。「認識」「消す」ボタン付き */
@Composable
fun HandwritingPad(
    enabled: Boolean,
    onRecognize: (strokes: List<List<InkPoint>>, width: Float, height: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strokes = remember { mutableStateListOf<List<InkPoint>>() }
    var current by remember { mutableStateOf<List<InkPoint>>(emptyList()) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFFFFFDF5))
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        current = listOf(InkPoint(down.position.x, down.position.y, down.uptimeMillis))
                        drag(down.id) { change ->
                            current = current + InkPoint(change.position.x, change.position.y, change.uptimeMillis)
                            change.consume()
                        }
                        strokes.add(current)
                        current = emptyList()
                    }
                },
        ) {
            (strokes + listOf(current)).forEach { pts ->
                for (i in 1 until pts.size) {
                    drawLine(
                        Color(0xFF222222),
                        Offset(pts[i - 1].first, pts[i - 1].second),
                        Offset(pts[i].first, pts[i].second),
                        strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round,
                    )
                }
            }
            // 目安の十字線
            val guide = Color(0x22000000)
            drawLine(guide, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height.toFloat()), 1.dp.toPx())
            drawLine(guide, Offset(0f, size.height / 2f), Offset(size.width.toFloat(), size.height / 2f), 1.dp.toPx())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { strokes.clear(); current = emptyList() }, Modifier.weight(1f)) {
                Text("消す")
            }
            Button(
                onClick = {
                    if (strokes.isNotEmpty()) {
                        onRecognize(strokes.toList(), size.width.toFloat(), size.height.toFloat())
                        strokes.clear()
                    }
                },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) { Text("認識") }
        }
    }
}
