package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.core.common.player.isExtraSourceUrl
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.utils.matchDubbingName

/**
 * Выбирает видео балансера из [playerVideos] одной серии: сначала озвучка, запомненная у тайтла
 * ([rememberedDubbing]), затем озвучка [targetDubbing], затем самая просматриваемая.
 * Запомненная озвучка могла называться иначе (канонизация списка меняется), поэтому после точного
 * совпадения пробуем [matchDubbingName].
 */
internal fun pickBalancerVideo(
    playerVideos: List<AnimeVideo>,
    rememberedDubbing: String?,
    targetDubbing: String,
): AnimeVideo? {
    val remembered = rememberedDubbing?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
        playerVideos.firstOrNull { it.dubbing.trim() == name }
            ?: matchDubbingName(name, playerVideos.map { it.dubbing })
                ?.let { matched -> playerVideos.firstOrNull { it.dubbing == matched } }
    }
    return remembered
        ?: playerVideos.firstOrNull { it.dubbing == targetDubbing }
        ?: playerVideos.maxByOrNull { it.views ?: 0 }
        ?: playerVideos.firstOrNull()
}

/** Видео принадлежит источнику-расширению (4K), озвучку которого мы запоминаем у тайтла. */
internal fun AnimeVideo.isExtraSourceVideo(): Boolean = iframeUrl.isExtraSourceUrl()
