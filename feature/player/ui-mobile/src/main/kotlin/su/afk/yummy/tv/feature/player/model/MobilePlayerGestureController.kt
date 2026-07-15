package su.afk.yummy.tv.feature.player.model

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import su.afk.yummy.tv.feature.player.utils.MOBILE_PLAYER_MIN_BRIGHTNESS
import su.afk.yummy.tv.feature.player.utils.calculateMobileVideoTransform
import su.afk.yummy.tv.feature.player.utils.readSystemBrightnessFraction
import kotlin.math.roundToInt

/**
 * Жесты мобильного плеера: яркость/громкость (вертикальный свайп),
 * pinch-zoom видео и speed-boost по long-press.
 */
@Stable
internal class MobilePlayerGestureController(
    private val activity: Activity?,
    private val context: Context,
    private val audioManager: AudioManager?,
    private val maxVolume: Int,
    initialTransform: MobileVideoTransform,
    private val onGestureStart: () -> Unit,
    private val onVideoTransformChanged: (MobileVideoTransform) -> Unit,
) {
    var liveVideoTransform: MobileVideoTransform by mutableStateOf(initialTransform)
    var transformGestureActive: Boolean by mutableStateOf(false)
    var playerSize: IntSize by mutableStateOf(IntSize.Zero)

    var isSpeedBoosted: Boolean by mutableStateOf(false)
        private set
    var brightnessGestureActive: Boolean by mutableStateOf(false)
        private set
    var brightnessLevel: Float by mutableFloatStateOf(0.5f)
        private set
    var volumeGestureActive: Boolean by mutableStateOf(false)
        private set
    var volumeLevel: Float by mutableFloatStateOf(0.5f)
        private set

    fun startTransformGesture() {
        transformGestureActive = true
        onGestureStart()
    }

    fun endTransformGesture() {
        transformGestureActive = false
    }

    fun startSpeedBoost() {
        isSpeedBoosted = true
        onGestureStart()
    }

    fun endSpeedBoost() {
        isSpeedBoosted = false
    }

    fun startVerticalGesture(zone: MobileVerticalGestureZone) {
        onGestureStart()
        when (zone) {
            MobileVerticalGestureZone.Brightness -> {
                brightnessLevel = activity?.window?.attributes?.screenBrightness
                    ?.takeIf { it in 0f..1f }
                    ?: readSystemBrightnessFraction(context)
                brightnessGestureActive = true
            }

            MobileVerticalGestureZone.Volume -> {
                volumeLevel = audioManager
                    ?.getStreamVolume(AudioManager.STREAM_MUSIC)
                    ?.div(maxVolume.toFloat())
                    ?: 0f
                volumeGestureActive = true
            }
        }
    }

    fun applyVerticalGesture(zone: MobileVerticalGestureZone, deltaFraction: Float) {
        when (zone) {
            MobileVerticalGestureZone.Brightness -> {
                brightnessLevel = (brightnessLevel + deltaFraction)
                    .coerceIn(MOBILE_PLAYER_MIN_BRIGHTNESS, 1f)
                activity?.window?.let { window ->
                    window.attributes = window.attributes.apply {
                        screenBrightness = brightnessLevel
                    }
                }
            }

            MobileVerticalGestureZone.Volume -> {
                volumeLevel = (volumeLevel + deltaFraction).coerceIn(0f, 1f)
                audioManager?.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    (volumeLevel * maxVolume).roundToInt(),
                    0,
                )
            }
        }
    }

    fun endVerticalGesture(zone: MobileVerticalGestureZone) {
        when (zone) {
            MobileVerticalGestureZone.Brightness -> brightnessGestureActive = false
            MobileVerticalGestureZone.Volume -> volumeGestureActive = false
        }
    }

    fun applyVideoTransform(centroid: Offset, pan: Offset, zoomChange: Float) {
        val transform = calculateMobileVideoTransform(
            currentScale = liveVideoTransform.scale,
            currentOffset = liveVideoTransform.offset,
            playerSize = playerSize,
            centroid = centroid,
            pan = pan,
            zoomChange = zoomChange,
        )
        liveVideoTransform = transform
        onVideoTransformChanged(transform)
    }

    fun resetForPictureInPicture() {
        transformGestureActive = false
        isSpeedBoosted = false
        brightnessGestureActive = false
        volumeGestureActive = false
    }
}

@Composable
internal fun rememberMobilePlayerGestureController(
    activity: Activity?,
    initialTransform: MobileVideoTransform,
    onGestureStart: () -> Unit,
    onVideoTransformChanged: (MobileVideoTransform) -> Unit,
): MobilePlayerGestureController {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.takeIf { it > 0 } ?: 1
    }
    val currentOnGestureStart = rememberUpdatedState(onGestureStart)
    val currentOnVideoTransformChanged = rememberUpdatedState(onVideoTransformChanged)
    val controller = remember {
        MobilePlayerGestureController(
            activity = activity,
            context = context,
            audioManager = audioManager,
            maxVolume = maxVolume,
            initialTransform = initialTransform,
            onGestureStart = { currentOnGestureStart.value.invoke() },
            onVideoTransformChanged = { currentOnVideoTransformChanged.value.invoke(it) },
        )
    }
    DisposableEffect(activity) {
        val originalBrightness = activity?.window?.attributes?.screenBrightness
        onDispose {
            activity?.window?.let { window ->
                window.attributes = window.attributes.apply {
                    screenBrightness = originalBrightness
                        ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }
    return controller
}
