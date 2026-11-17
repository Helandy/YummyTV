package su.afk.yummy.tv.feature.player.common.utils

import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.util.UnstableApi
import su.afk.yummy.tv.core.model.settings.VideoEnhancementMode
import su.afk.yummy.tv.feature.player.common.service.Anime4kRestoreGlEffect
import su.afk.yummy.tv.feature.player.common.service.DeblurDogGlEffect

/**
 * Эффекты режима улучшения видео; для [VideoEnhancementMode.OFF] — пусто. Оба эффекта работают на исходном
 * разрешении кадра. Апскейла в цепочке нет: бикубик на 4K-кадре (16 выборок на пиксель) не тянут GPU
 * ТВ-приставок, картинка отстаёт от звука. До размера экрана кадр растягивает аппаратный скейлер вывода.
 */
@OptIn(UnstableApi::class)
internal fun VideoEnhancementMode.videoEffects(): List<Effect> = when (this) {
    VideoEnhancementMode.OFF -> emptyList()
    VideoEnhancementMode.SHARPEN -> listOf(DeblurDogGlEffect())
    VideoEnhancementMode.QUALITY -> listOf(Anime4kRestoreGlEffect())
}
