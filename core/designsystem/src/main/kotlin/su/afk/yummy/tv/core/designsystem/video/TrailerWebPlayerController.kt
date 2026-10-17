package su.afk.yummy.tv.core.designsystem.video

import android.webkit.WebView

/** Управление встроенным веб-плеером трейлера: пауза/старт и перемотка кнопками пульта. */
class TrailerWebPlayerController {
    internal var webView: WebView? = null

    /** Переключает паузу и воспроизведение. */
    fun togglePlayback() = run("toggle()")

    /** Перематывает на [seconds] секунд; отрицательное значение — назад. */
    fun seekBy(seconds: Int) = run("seekBy($seconds)")

    private fun run(script: String) {
        webView?.evaluateJavascript(script, null)
    }
}
