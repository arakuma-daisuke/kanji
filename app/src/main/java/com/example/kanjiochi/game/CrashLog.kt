package com.example.kanjiochi.game

import android.content.Context
import android.util.Log
import java.io.File

/** 落ちた原因を端末内に保存し、次回起動時に画面へ表示するための簡易ログ */
object CrashLog {
    private var file: File? = null

    /** 次回起動時に表示する内容。表示後は消す */
    var pending: String? = null
        private set

    fun install(context: Context) {
        val f = File(context.filesDir, "crash.txt")
        file = f
        if (f.exists()) {
            pending = runCatching { f.readText() }.getOrNull()
            f.delete()
        }
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { f.writeText(Log.getStackTraceString(e)) }
            old?.uncaughtException(t, e)
        }
    }

    /** 落ちはしないが失敗した内容を記録する */
    fun record(label: String, e: Throwable) {
        Log.e("KanjiOchi", label, e)
        pending = "$label\n" + Log.getStackTraceString(e)
    }

    fun clear() { pending = null }
}
