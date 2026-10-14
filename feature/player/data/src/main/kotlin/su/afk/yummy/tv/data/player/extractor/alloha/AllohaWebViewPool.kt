package su.afk.yummy.tv.data.player.extractor.alloha

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import su.afk.yummy.tv.data.player.extractor.alloha.AllohaWebViewPool.Companion.MAX_IDLE_WEB_VIEWS
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Blocks a short, explicit list of well-known third-party ad/tracker hosts the Alloha player page
 * pulls in for its own ad breaks - fail-open by design: anything not on [AD_HOST_BLOCKLIST] is
 * left completely alone. Alloha's own domain and its CDN hosts are never in this list and must
 * never be added to it - blocking the player's own requests (rather than third-party ad ones) has
 * broken session extraction before, see AllohaWrapperScript's comment on the master.m3u8 capture.
 * This is a defense-in-depth measure alongside the media-mute logic in [wrapperHtml] - it reduces
 * how often an ad loads at all, it doesn't replace muting whatever does get through.
 */
private val AD_HOST_BLOCKLIST = setOf(
    "doubleclick.net",
    "googlesyndication.com",
    "adnxs.com",
    "criteo.com",
    "taboola.com",
    "outbrain.com",
    "popads.net",
    "propellerads.com",
    "adsterra.com",
    "exoclick.com",
    "juicyads.com",
    "mgid.com",
)

private class AdBlockingWebViewClient : WebViewClient() {
    private val blockedResponse by lazy {
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val host = request.url.host ?: return null
        val isBlocked =
            AD_HOST_BLOCKLIST.any { blockedHost -> host == blockedHost || host.endsWith(".$blockedHost") }
        return if (isBlocked) blockedResponse else null
    }
}

/**
 * Booting a fresh WebView (and its underlying Chromium renderer) is the single most expensive
 * step in opening an Alloha session - a cold start that retries 2-3x (each retry tearing down
 * and recreating the WebView from scratch) pays that cost every time. The reference
 * implementation avoids this by keeping one WebView alive for the whole activity lifetime.
 * We use a small pool (capped at [MAX_IDLE_WEB_VIEWS]) instead of a single shared instance so a
 * second concurrent caller (e.g. a background download running while an episode is playing)
 * can't collide with an in-use WebView - it simply gets its own fresh instance, which it
 * destroys again on release instead of returning it to an already-full pool.
 */
internal class AllohaWebViewPool {
    private val idleWebViews = ConcurrentLinkedQueue<WebView>()

    @SuppressLint("SetJavaScriptEnabled")
    fun acquire(context: Context, userAgent: String): WebView {
        val webView = idleWebViews.poll() ?: WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = AdBlockingWebViewClient()
        }
        webView.settings.userAgentString = userAgent
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
        return webView
    }

    /**
     * Drops everything the site could use to recognise this install: cookies, DOM storage, and the
     * idle (warm) WebViews that still hold them. Main thread only. Used after the CDN rejected a
     * session's identity - every later "fresh" session otherwise inherits the same flagged cookie
     * jar and is rejected the same way.
     */
    fun resetIdentity() {
        generateSequence { idleWebViews.poll() }.forEach(WebView::destroy)
        CookieManager.getInstance().apply {
            removeAllCookies(null)
            flush()
        }
        WebStorage.getInstance().deleteAllData()
    }

    fun release(webView: WebView) {
        if (idleWebViews.size >= MAX_IDLE_WEB_VIEWS) {
            webView.destroy()
        } else {
            idleWebViews.offer(webView)
        }
    }

    private companion object {
        // How many idle WebViews acquire()/release() keep warm for reuse. 1 covers the common
        // case (sequential recovery retries / episode switches); extra concurrent callers get
        // their own instance and destroy it on release rather than growing the pool.
        const val MAX_IDLE_WEB_VIEWS = 1
    }
}
