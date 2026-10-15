package su.afk.yummy.tv.feature.player.mobile.model

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import su.afk.yummy.tv.feature.player.common.PlayerBrightnessController
import su.afk.yummy.tv.feature.player.common.rememberPlayerBrightnessController
import su.afk.yummy.tv.feature.player.mobile.utils.MOBILE_PLAYER_MIN_BRIGHTNESS
import su.afk.yummy.tv.feature.player.mobile.utils.calculateMobileVideoTransform
import su.afk.yummy.tv.feature.player.mobile.utils.readSystemBrightnessFraction
import kotlin.math.roundToInt

/** Шаг изменения яркости/громкости при вертикальном свайпе. */
private const val VERTICAL_GESTURE_STEP = 0.05f

/**
 * Жесты мобильного плеера: яркость/громкость (вертикальный свайп),
 * pinch-zoom видео и speed-boost по long-press.
 */
@Stable
internal class MobilePlayerGestureController(
    private val activity: Activity?,
    private val context: Context,
    initialTransform: MobileVideoTransform,
    private val volumeLevelProvider: () -> Float,
    private val onVolumeChanged: (Float) -> Unit,
    private val brightnessController: PlayerBrightnessController,
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

    // непрерывное значение свайпа; наружу выдаётся квантованное по VERTICAL_GESTURE_STEP
    private var rawBrightnessLevel = 0.5f
    private var rawVolumeLevel = 0.5f

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
                rawBrightnessLevel = activity?.window?.attributes?.screenBrightness
                    ?.takeIf { it in 0f..1f }
                    ?: readSystemBrightnessFraction(context)
                brightnessLevel = rawBrightnessLevel
                brightnessGestureActive = true
            }

            MobileVerticalGestureZone.Volume -> {
                rawVolumeLevel = volumeLevelProvider().coerceIn(0f, 1f)
                volumeLevel = rawVolumeLevel
                volumeGestureActive = true
            }
        }
    }

    fun applyVerticalGesture(zone: MobileVerticalGestureZone, deltaFraction: Float) {
        when (zone) {
            MobileVerticalGestureZone.Brightness -> {
                rawBrightnessLevel = (rawBrightnessLevel + deltaFraction)
                    .coerceIn(MOBILE_PLAYER_MIN_BRIGHTNESS, 1f)
                val stepped = rawBrightnessLevel.quantizeToGestureStep()
                    .coerceIn(MOBILE_PLAYER_MIN_BRIGHTNESS, 1f)
                if (stepped == brightnessLevel) return
                brightnessLevel = stepped
                brightnessController.setFraction(stepped)
                activity?.window?.let { window ->
                    window.attributes = window.attributes.apply {
                        screenBrightness = brightnessLevel
                    }
                }
            }

            MobileVerticalGestureZone.Volume -> {
                rawVolumeLevel = (rawVolumeLevel + deltaFraction).coerceIn(0f, 1f)
                val stepped = rawVolumeLevel.quantizeToGestureStep().coerceIn(0f, 1f)
                if (stepped == volumeLevel) return
                volumeLevel = stepped
                onVolumeChanged(volumeLevel)
            }
        }
    }

    private fun Float.quantizeToGestureStep(): Float =
        (this / VERTICAL_GESTURE_STEP).roundToInt() * VERTICAL_GESTURE_STEP

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
    volumeLevelProvider: () -> Float,
    onVolumeChanged: (Float) -> Unit,
    onGestureStart: () -> Unit,
    onVideoTransformChanged: (MobileVideoTransform) -> Unit,
): MobilePlayerGestureController {
    val context = LocalContext.current
    val brightnessController = rememberPlayerBrightnessController()
    val currentVolumeLevelProvider = rememberUpdatedState(volumeLevelProvider)
    val currentOnVolumeChanged = rememberUpdatedState(onVolumeChanged)
    val currentOnGestureStart = rememberUpdatedState(onGestureStart)
    val currentOnVideoTransformChanged = rememberUpdatedState(onVideoTransformChanged)
    val controller = remember {
        MobilePlayerGestureController(
            activity = activity,
            context = context,
            initialTransform = initialTransform,
            brightnessController = brightnessController,
            volumeLevelProvider = { currentVolumeLevelProvider.value.invoke() },
            onVolumeChanged = { currentOnVolumeChanged.value.invoke(it) },
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
    // Сохранённая яркость применяется поверх системной; пока её нет, окно остаётся на системной.
    val savedBrightness by brightnessController.brightness.collectAsState()
    LaunchedEffect(activity, savedBrightness) {
        val level = savedBrightness ?: return@LaunchedEffect
        activity?.window?.let { window ->
            window.attributes = window.attributes.apply { screenBrightness = level }
        }
    }
    return controller
}
