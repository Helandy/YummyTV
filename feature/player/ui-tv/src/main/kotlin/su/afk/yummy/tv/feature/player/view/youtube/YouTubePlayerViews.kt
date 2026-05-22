package su.afk.yummy.tv.feature.player.view.youtube

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay

@Composable
internal fun YouTubeTrailerView(
    iframeUrl: String,
    screenshotUrls: List<String>,
) {
    var hasError by remember(iframeUrl) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasError && screenshotUrls.isNotEmpty()) {
            ScreenshotSlideshowView(screenshotUrls = screenshotUrls)
        } else if (!hasError) {
            YouTubeWebPlayer(iframeUrl = iframeUrl, onError = { hasError = true })
        }
    }
}

@Composable
private fun YouTubeWebPlayer(iframeUrl: String, onError: () -> Unit) {
    val videoId = remember(iframeUrl) {
        iframeUrl.substringAfter("/embed/").substringBefore("?")
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val playerView = remember(videoId) {
        val options = IFramePlayerOptions.Builder(context)
            .controls(1)
            .autoplay(1)
            .rel(0)
            .build()

        YouTubePlayerView(context).apply {
            enableAutomaticInitialization = false
            initialize(
                object : AbstractYouTubePlayerListener() {
                    override fun onReady(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.loadVideo(videoId, 0f)
                    }
                    override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                        onError()
                    }
                },
                options,
            )
        }
    }

    DisposableEffect(playerView) {
        lifecycleOwner.lifecycle.addObserver(playerView)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(playerView)
            playerView.release()
        }
    }

    AndroidView(
        factory = { playerView },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun ScreenshotSlideshowView(screenshotUrls: List<String>) {
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(screenshotUrls) {
        while (true) {
            delay(3_000)
            index = (index + 1) % screenshotUrls.size
        }
    }
    AnimatedContent(
        targetState = screenshotUrls.getOrNull(index),
        transitionSpec = { fadeIn(tween(800)) togetherWith fadeOut(tween(800)) },
        label = "screenshot_slideshow",
    ) { url ->
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().background(Color.Black),
        )
    }
}
