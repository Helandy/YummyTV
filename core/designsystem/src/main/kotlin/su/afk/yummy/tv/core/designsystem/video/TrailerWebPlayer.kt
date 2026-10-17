package su.afk.yummy.tv.core.designsystem.video

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val YOUTUBE_ORIGIN = "https://yummytv.app"

/**
 * Встроенный плеер трейлера. YouTube играет через IFrame API, загруженный с origin youtube.com
 * (иначе embed отвечает ошибкой 152/153); остальные источники открываются как есть по [embedUrl].
 *
 * [onError] вызывается, когда встроить видео не удалось: embed запрещён владельцем или страница
 * не загрузилась.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrailerWebPlayer(
    videoId: String?,
    embedUrl: String,
    controller: TrailerWebPlayerController,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnError by rememberUpdatedState(onError)

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                webChromeClient = WebChromeClient()
                val mainHandler = Handler(Looper.getMainLooper())
                addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun onPlayerError() {
                            mainHandler.post { currentOnError() }
                        }
                    },
                    "TrailerBridge",
                )
                if (videoId != null) {
                    loadDataWithBaseURL(YOUTUBE_ORIGIN, youtubePage(videoId), "text/html", "utf-8", null)
                } else {
                    loadUrl(embedUrl)
                }
                controller.webView = this
            }
        },
        onRelease = { view ->
            controller.webView = null
            view.stopLoading()
            view.destroy()
        },
    )

    DisposableEffect(controller) {
        onDispose { controller.webView = null }
    }
}

private fun youtubePage(videoId: String): String = """
    <html><head><meta name="viewport" content="width=device-width, initial-scale=1">
    <style>html,body{margin:0;height:100%;background:#000}iframe{position:fixed;inset:0;width:100%;height:100%;border:0}</style>
    </head><body><div id="p"></div>
    <script src="https://www.youtube.com/iframe_api"></script>
    <script>
    var player;
    function onYouTubeIframeAPIReady(){
      player = new YT.Player('p', {
        videoId: '$videoId',
        playerVars: {autoplay: 1, playsinline: 1, rel: 0, fs: 0, enablejsapi: 1, origin: "https://yummytv.app"},
        events: {
          onReady: function(e){ e.target.playVideo(); },
          onError: function(){ TrailerBridge.onPlayerError(); }
        }
      });
    }
    function toggle(){ if(player){ if(player.getPlayerState()==1){player.pauseVideo();}else{player.playVideo();} } }
    function seekBy(s){ if(player){ player.seekTo(player.getCurrentTime()+s, true); } }
    </script></body></html>
""".trimIndent()
