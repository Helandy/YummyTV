package su.afk.yummy.tv.feature.player.common.utils

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import su.afk.yummy.tv.feature.player.common.service.DeblurGlEffect
import su.afk.yummy.tv.feature.player.common.service.UpscaleGlEffect

private const val FALLBACK_DISPLAY_HEIGHT = 1080
private const val MAX_UPSCALE_HEIGHT = 2160

/**
 * Цепочка эффектов Deblur: резкость с порогом на исходном разрешении (там подобраны сила и порог), затем апскейл
 * Catmull-Rom до высоты экрана вместо билинейного растяжения SurfaceView. Обратный порядок проверен — резкость
 * на выходном разрешении слабее и захватывает не те детали.
 */
@OptIn(UnstableApi::class)
internal fun Context.deblurVideoEffects(): List<Effect> =
    listOf(DeblurGlEffect(), UpscaleGlEffect(displayVideoHeight()))

/** Короткая сторона физического режима дисплея — высота кадра в ландшафте; ограничена 4K. */
private fun Context.displayVideoHeight(): Int {
    val mode = getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY)?.mode
    val height = mode?.let { minOf(it.physicalWidth, it.physicalHeight) } ?: FALLBACK_DISPLAY_HEIGHT
    return height.coerceIn(1, MAX_UPSCALE_HEIGHT)
}
