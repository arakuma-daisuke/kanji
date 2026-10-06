package com.example.kanjiochi.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kanjiochi.game.CrashLog
import com.example.kanjiochi.game.InkRecognizer
import com.example.kanjiochi.model.*

private val Night = Color(0xFF14161F)

@Composable
fun StartScreen(
    selected: Difficulty,
    mode: QuestionMode,
    modelState: InkRecognizer.ModelState,
    onSelect: (Difficulty) -> Unit,
    onSelectMode: (QuestionMode) -> Unit,
    onStart: () -> Unit,
    onRetryModel: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Night).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("漢字サドンデス", color = Color(0xFFFFB300), fontSize = 36.sp)
        Text("爆弾が落ちる前に答えろ！", color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.height(32.dp))
        Text("難易度", color = Color.White)
        Spacer(Modifier.height(8.dp))
        Difficulty.values().toList().chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d ->
                    val on = d == selected
                    Button(
                        onClick = { onSelect(d) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (on) Color(0xFFE24B4A) else Color(0xFF3A3D4D),
                        ),
                        modifier = Modifier.width(130.dp),
                    ) { Text(d.label) }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("出題モード", color = Color.White)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuestionMode.values().forEach { m ->
                Button(
                    onClick = { onSelectMode(m) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (m == mode) Color(0xFFE24B4A) else Color(0xFF3A3D4D),
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.weight(1f),
                ) { Text(m.label, fontSize = 13.sp, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth(0.7f).height(52.dp)) {
            Text("スタート", fontSize = 20.sp)
        }
        Spacer(Modifier.height(16.dp))
        var report by remember { mutableStateOf(CrashLog.pending) }
        report?.let { text ->
            Text("前回のエラー内容（開発者に見せてください）", color = Color(0xFFFF8A80), fontSize = 12.sp)
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text.take(1500), color = Color.LightGray, fontSize = 9.sp,
                    modifier = Modifier.heightIn(max = 160.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                )
            }
            TextButton(onClick = { CrashLog.clear(); report = null }) { Text("エラー表示を閉じる") }
        }
        when (modelState) {
            InkRecognizer.ModelState.LOADING ->
                Text("手書きモデルを準備中…（初回のみダウンロード）", color = Color.LightGray, fontSize = 12.sp)
            InkRecognizer.ModelState.READY ->
                Text("手書きモデル: 準備OK", color = Color(0xFF7BD88F), fontSize = 12.sp)
            InkRecognizer.ModelState.FAILED -> {
                Text("手書きモデルのDLに失敗（書き問題は認識できません）", color = Color(0xFFFF8A80), fontSize = 12.sp)
                TextButton(onClick = onRetryModel) { Text("再試行") }
            }
        }
    }
}

@Composable
fun GameOverScreen(state: GameUiState, onRetry: () -> Unit, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Night).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("GAME OVER", color = Color(0xFFE24B4A), fontSize = 40.sp)
        Spacer(Modifier.height(16.dp))
        Text("スコア  ${state.score}", color = Color.White, fontSize = 28.sp)
        Text("最大コンボ  ${state.maxCombo}", color = Color(0xFFFFB300), fontSize = 18.sp)
        Text("難易度: ${state.difficulty.label} / ${state.mode.label}", color = Color.LightGray, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(0.7f).height(52.dp)) {
            Text("もう一度", fontSize = 20.sp)
        }
        TextButton(onClick = onBack) { Text("難易度選択へ") }
    }
}

/** 上半分: 落下エリア / 下半分: 回答エリア */
@Composable
fun PlayScreen(
    state: GameUiState,
    modelReady: Boolean,
    onSubmitReading: (String) -> Unit,
    onRecognize: (List<List<InkPoint>>, Float, Float) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Night).statusBarsPadding().imePadding()) {
        FallArea(state, Modifier.weight(1f).fillMaxWidth())
        AnswerArea(state, modelReady, onSubmitReading, onRecognize, Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun FallArea(state: GameUiState, modifier: Modifier) {
    BoxWithConstraints(modifier.background(Color(0xFF1E2233))) {
        val target = state.target
        state.bombs.forEach { b ->
            val x = (maxWidth - BombStyle.Width) * b.x
            val y = (maxHeight - BombStyle.Height) * b.progress
            Box(Modifier.offset(x = x, y = y)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BombView(b, isTarget = b.id == target?.id)
                }
            }
        }
        // 下端の警告ライン
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(Color(0xFFE24B4A)))
        // HUD
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("♥".repeat(state.lives), color = Color(0xFFFF5252), fontSize = 20.sp)
            Column(horizontalAlignment = Alignment.End) {
                Text("SCORE ${state.score}", color = Color.White, fontSize = 16.sp)
                if (state.combo > 0) {
                    Text("${state.combo} COMBO  x${state.multiplier}", color = Color(0xFFFFB300), fontSize = 14.sp)
                }
            }
        }
        if (state.message.isNotEmpty()) {
            Text(
                state.message, color = Color.White, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
            )
        }
        // 下端到達時の赤フラッシュ
        if (state.flash > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.5f * state.flash)))
        }
    }
}

@Composable
private fun AnswerArea(
    state: GameUiState,
    modelReady: Boolean,
    onSubmitReading: (String) -> Unit,
    onRecognize: (List<List<InkPoint>>, Float, Float) -> Unit,
    modifier: Modifier,
) {
    val target = state.target
    Column(modifier.background(Color(0xFF0F111A)).padding(12.dp)) {
        when (target?.question?.type) {
            QuestionType.READ, null -> ReadInput(onSubmitReading, target != null)
            QuestionType.WRITE -> {
                val q = target.question
                Text(q.sentence, color = Color.White, fontSize = 16.sp)
                Text(
                    "${state.writeIndex + 1}文字目を書いてください（全${q.kanji.length}文字）" +
                        if (!modelReady) "  ※モデル未準備" else "",
                    color = Color(0xFFFFB300), fontSize = 12.sp,
                )
                Spacer(Modifier.height(6.dp))
                HandwritingPad(modelReady, onRecognize, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ReadInput(onSubmit: (String) -> Unit, active: Boolean) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    fun submit() {
        onSubmit(text)
        text = ""
    }
    Text(
        if (active) "落ちてくる熟語の読みを、ひらがなで入力" else "爆弾を待っています…",
        color = Color.LightGray, fontSize = 14.sp,
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp, color = Color.White, textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
    Spacer(Modifier.height(8.dp))
    Button(onClick = { submit() }, modifier = Modifier.fillMaxWidth()) { Text("回答") }
}
