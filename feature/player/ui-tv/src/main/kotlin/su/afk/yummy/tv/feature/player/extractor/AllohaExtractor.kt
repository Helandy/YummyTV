package su.afk.yummy.tv.feature.player.extractor

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.feature.player.view.CHROME_UA
import kotlin.coroutines.resume

internal data class AllohaResult(val url: String, val headers: Map<String, String>)

// Alloha iframe pages are JS-rendered SPAs.
// The fileList is server-rendered into <script> but stream URLs require
// a signed "Borth" header computed by obfuscated JS — static parsing can't get them.
// We load the page in a hidden WebView and intercept the HLS m3u8 request.
internal object AllohaExtractor {

    private const val TIMEOUT_MS = 25_000L

    suspend fun extract(iframeUrl: String, context: Context): AllohaResult? {
        val fullUrl = normalizeUrl(iframeUrl)
        return withContext(Dispatchers.Main) {
            extractViaWebView(fullUrl, context)
        }
    }

    private suspend fun extractViaWebView(iframeUrl: String, context: Context): AllohaResult? =
        suspendCancellableCoroutine { cont ->
            var webView: WebView? = null
            var delivered = false
            val handler = Handler(Looper.getMainLooper())

            fun deliver(result: AllohaResult?) {
                if (!delivered) {
                    delivered = true
                    val wv = webView
                    webView = null
                    handler.post { wv?.destroy() }
                    if (cont.isActive) cont.resume(result)
                }
            }

            val timeoutRunnable = Runnable {
                deliver(null)
            }
            handler.postDelayed(timeoutRunnable, TIMEOUT_MS)

            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    @Suppress("DEPRECATION")
                    allowFileAccess = false
                    mediaPlaybackRequiresUserGesture = false
                    userAgentString = CHROME_UA
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        val url = request.url.toString()
                        if (isStreamUrl(url)) {
                            handler.removeCallbacks(timeoutRunnable)
                            deliver(AllohaResult(url = url, headers = request.requestHeaders))
                        }
                        return null
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        // Trigger autoplay — userParam.autoplay may be 0
                        view.evaluateJavascript(
                            """try{var v=document.getElementById('player');if(v)v.play().catch(function(){});}catch(e){}""",
                            null,
                        )
                    }
                }

                // Wrap in iframe so the Alloha page sees isFramed=true and doesn't remove itself
                val html = wrapperHtml(iframeUrl)
                loadDataWithBaseURL(
                    "https://alloha.yani.tv/",
                    html,
                    "text/html",
                    "utf-8",
                    null,
                )
            }

            cont.invokeOnCancellation {
                handler.removeCallbacks(timeoutRunnable)
                deliver(null)
            }
        }

    private fun isStreamUrl(url: String): Boolean {
        if (!url.contains(".m3u8")) return false
        // Skip ad/analytics m3u8s
        return !url.contains("ima") && !url.contains("doubleclick") && !url.contains("ads")
    }

    private fun wrapperHtml(iframeUrl: String): String {
        val escaped = iframeUrl.replace("&", "&amp;").replace("\"", "&quot;")
        return """<!DOCTYPE html><html><head>
            <meta charset="utf-8">
            <style>*{margin:0;padding:0}html,body,iframe{width:100%;height:100%;border:none;background:#000}</style>
            </head><body>
            <iframe src="$escaped" allow="autoplay;fullscreen" allowfullscreen></iframe>
            </body></html>"""
    }

    private fun normalizeUrl(url: String) = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http") -> url
        else -> "https://$url"
    }
}
