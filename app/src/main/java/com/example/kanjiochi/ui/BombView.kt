package com.example.kanjiochi.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kanjiochi.model.Bomb
import com.example.kanjiochi.model.QuestionType

object BombStyle {
    val BodyNormal = Color(0xFF2C2C2A)
    val BodyDanger = Color(0xFF791F1F)
    val RimDanger = Color(0xFFE24B4A)
    val RimNormal = Color(0xFF5F5E5A)
    val Text = Color(0xFFF1EFE8)
    val Spark = Color(0xFFFF9A1F)
    val Cap = Color(0xFF9A9A94)

    val Width: Dp = 104.dp
    val Height: Dp = 124.dp

    const val BLINK_NORMAL_MS = 200 // 往復で約0.4秒周期
    const val BLINK_DANGER_MS = 75  // 往復で約0.15秒周期
    const val SWAY_DP = 3f
    const val READ_SP = 20
    const val WRITE_SP = 13
}

/** 爆弾1個（画像素材は使わずCanvasで描画） */
@Composable
fun BombView(bomb: Bomb, isTarget: Boolean, modifier: Modifier = Modifier) {
    val danger = bomb.isDanger
    val transition = rememberInfiniteTransition(label = "bomb")
    val blink by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (danger) BombStyle.BLINK_DANGER_MS else BombStyle.BLINK_NORMAL_MS, easing = LinearEasing),
            RepeatMode.Reverse,
        ),
        label = "blink",
    )
    val sway by transition.animateFloat(
        initialValue = -BombStyle.SWAY_DP, targetValue = BombStyle.SWAY_DP,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "sway",
    )
    val e = bomb.explosion
    val scale = if (e != null) 1f + e * 1.2f else 1f
    val fade = if (e != null) 1f - e else 1f

    Box(
        modifier = modifier
            .size(BombStyle.Width, BombStyle.Height)
            .offset(x = if (e == null) sway.dp else 0.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .alpha(fade),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(Modifier.size(BombStyle.Width, BombStyle.Height)) {
            val w = size.width
            val r = 40.dp.toPx()
            val center = Offset(w / 2, size.height - r - 2.dp.toPx())
            val body = if (danger) BombStyle.BodyDanger else BombStyle.BodyNormal
            val rim = if (danger) BombStyle.RimDanger else BombStyle.RimNormal

            // 導火線
            val capTop = center.y - r - 8.dp.toPx()
            val fuse = Path().apply {
                moveTo(w / 2, capTop)
                quadraticTo(w / 2 + 6.dp.toPx(), capTop - 12.dp.toPx(), w / 2 + 16.dp.toPx(), capTop - 14.dp.toPx())
            }
            drawPath(fuse, Color(0xFFB08A5A), style = Stroke(width = 3.dp.toPx()))

            // 火花（点滅: 透明度とサイズを交互に変える）
            val sparkBase = if (danger) 9.dp.toPx() else 5.dp.toPx()
            val sparkR = sparkBase * (0.7f + 0.6f * blink)
            val sparkColor = if (danger) Color(0xFFFF3B30) else BombStyle.Spark
            val sp = Offset(w / 2 + 16.dp.toPx(), capTop - 14.dp.toPx())
            drawCircle(sparkColor.copy(alpha = 0.3f + 0.4f * blink), sparkR * 1.8f, sp)
            drawCircle(sparkColor.copy(alpha = 0.4f + 0.6f * blink), sparkR, sp)
            drawCircle(Color.White.copy(alpha = 0.8f * blink), sparkR * 0.4f, sp)

            // 胴体
            drawCircle(body, r, center)
            drawCircle(rim, r, center, style = Stroke(width = 3.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.12f), r * 0.22f, center + Offset(-r * 0.45f, -r * 0.5f))
            if (isTarget) {
                drawCircle(Color(0xFFFFD60A), r + 5.dp.toPx(), center, style = Stroke(width = 2.dp.toPx()))
            }

            // 金属キャップ
            drawRoundRect(
                BombStyle.Cap,
                topLeft = Offset(w / 2 - 12.dp.toPx(), capTop),
                size = Size(24.dp.toPx(), 12.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
            )

            // 爆発の輪
            if (e != null) {
                drawCircle(Color(0xFFFFB300).copy(alpha = 1f - e), r * (1f + e), center, style = Stroke(6.dp.toPx()))
            }
        }
        val isRead = bomb.question.type == QuestionType.READ
        Text(
            text = if (isRead) bomb.question.kanji else katakanaOf(bomb),
            color = BombStyle.Text,
            fontSize = (if (isRead) BombStyle.READ_SP else BombStyle.WRITE_SP).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset(y = (-42).dp),
        )
    }
}

/** 書き問題で爆弾に出す文字（読みをカタカナにしたもの） */
private fun katakanaOf(b: Bomb): String =
    b.question.reading.map { if (it in 'ぁ'..'ゖ') (it + 0x60) else it }.joinToString("")
