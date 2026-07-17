package su.afk.yummy.tv.feature.account.view

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun AccountMobileHCaptcha(
    siteKey: String,
    onSolved: (String) -> Unit,
    onExpired: () -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val html = remember(siteKey) { mobileCaptchaHtml(siteKey) }
    var loading by remember(siteKey) { mutableStateOf(true) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            loading = true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            loading = false
                        }
                    }
                    webChromeClient = WebChromeClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    addJavascriptInterface(
                        MobileCaptchaBridge(onSolved, onExpired, onFailed),
                        "YummyCaptcha"
                    )
                    loadDataWithBaseURL("https://yummyani.me/", html, "text/html", "utf-8", null)
                }
            },
        )
        if (loading) CircularProgressIndicator()
    }
}

private class MobileCaptchaBridge(
    private val onSolved: (String) -> Unit,
    private val onExpired: () -> Unit,
    private val onFailed: () -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onSolved(token: String) {
        handler.post { onSolved(token) }
    }

    @JavascriptInterface
    fun onExpired() {
        handler.post { onExpired() }
    }

    @JavascriptInterface
    fun onError() {
        handler.post { onFailed() }
    }
}

private fun mobileCaptchaHtml(siteKey: String) = """
    <!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>html,body{margin:0;background:transparent}body{display:flex;justify-content:center}</style>
    <script>
      function solved(token){window.YummyCaptcha.onSolved(token||"")}
      function expired(){window.YummyCaptcha.onExpired()}
      function failed(){window.YummyCaptcha.onError()}
    </script><script src="https://js.hcaptcha.com/1/api.js" async defer></script></head>
    <!-- data-theme фиксирован тёмным: приложение не имеет светлой темы (см. YummyTvTheme). -->
    <body><div class="h-captcha" data-sitekey="$siteKey" data-theme="dark"
    data-callback="solved" data-expired-callback="expired" data-error-callback="failed"></div></body></html>
""".trimIndent()
