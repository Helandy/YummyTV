package su.afk.yummy.tv.feature.account.view

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun HCaptchaWebView(
    siteKey: String,
    onSolved: (String) -> Unit,
    onExpired: () -> Unit,
    onFailed: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val html = remember(siteKey) { hCaptchaHtml(siteKey) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isFocusable = true
                isFocusableInTouchMode = true
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                addJavascriptInterface(
                    CaptchaBridge(
                        onSolved = onSolved,
                        onExpired = onExpired,
                        onFailed = onFailed,
                    ),
                    "YummyCaptcha",
                )
                loadDataWithBaseURL(
                    "https://yummyani.me/",
                    html,
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
    )
}

private class CaptchaBridge(
    private val onSolved: (String) -> Unit,
    private val onExpired: () -> Unit,
    private val onFailed: (String?) -> Unit,
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
    fun onError(message: String?) {
        handler.post { onFailed(message) }
    }
}

private fun hCaptchaHtml(siteKey: String): String = """
    <!doctype html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            min-height: 100%;
            background: transparent;
            color: white;
            overflow: hidden;
          }
          body {
            display: flex;
            align-items: center;
            justify-content: center;
          }
          .h-captcha {
            transform-origin: center;
          }
        </style>
        <script>
          function onCaptchaSolved(token) {
            window.YummyCaptcha.onSolved(token || "");
          }
          function onCaptchaExpired() {
            window.YummyCaptcha.onExpired();
          }
          function onCaptchaError(error) {
            window.YummyCaptcha.onError(error || null);
          }
        </script>
        <script src="https://js.hcaptcha.com/1/api.js" async defer></script>
      </head>
      <body>
        <div
          class="h-captcha"
          data-sitekey="$siteKey"
          data-theme="dark"
          data-callback="onCaptchaSolved"
          data-expired-callback="onCaptchaExpired"
          data-error-callback="onCaptchaError">
        </div>
      </body>
    </html>
""".trimIndent()
