package su.afk.yummy.tv.feature.details.details

import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.common.episode.episodeGroupKey
import su.afk.yummy.tv.core.common.player.isSupportedPlayerUrl
import su.afk.yummy.tv.core.common.player.balancerDisplayComparator
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.details.model.BalancerOption
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.model.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.utils.dubbingEpisodeCount
import su.afk.yummy.tv.feature.details.utils.isExtraSourceVideo
import su.afk.yummy.tv.feature.details.utils.pickBalancerVideo

/** Resolves whether the selected episode can open directly or needs a balancer picker. */
internal fun resolveDetailsPlayerSelection(
    video: AnimeVideo,
    allVideos: List<AnimeVideo>,
    rememberedAnime4kDubbing: String? = null,
): DetailsPlayerSelection {
    val episodeVideos = allVideos.filter {
        it.episode.episodeGroupKey() == video.episode.episodeGroupKey()
    }
    // Если целевая озвучка в этом эпизоде есть только на неподдерживаемых балансерах,
    // пересаживаемся на самую популярную озвучку среди поддерживаемых.
    val targetVideo = if (
        episodeVideos.any { it.dubbing == video.dubbing && it.iframeUrl.isSupportedPlayerUrl() }
    ) {
        video
    } else {
        episodeVideos
            .filter { it.iframeUrl.isSupportedPlayerUrl() }
            .groupBy { it.dubbing }
            .maxByOrNull { (_, dubbingVideos) -> dubbingVideos.sumOf { it.views ?: 0 } }
            ?.value
            ?.maxByOrNull { it.views ?: 0 }
            ?: video
    }

    val options = episodeVideos
        .groupBy { it.player }
        .entries
        .map { (playerName, playerVideos) ->
            // Запомненная озвучка применяется только к источнику 4K: у остальных балансеров
            // озвучка по-прежнему следует за целевым видео.
            val remembered = rememberedAnime4kDubbing
                .takeIf { playerVideos.first().isExtraSourceVideo() }
            val representative = pickBalancerVideo(
                playerVideos = playerVideos,
                rememberedDubbing = remembered,
                targetDubbing = targetVideo.dubbing,
            ) ?: playerVideos.first()
            BalancerOption(
                playerName = playerName,
                video = representative,
                isSupported = representative.iframeUrl.isSupportedPlayerUrl(),
                // Серии считаем по всему тайтлу: у балансера может быть двадцать серий,
                // хотя в пикер он попал из-за одной выбранной.
                episodeCount = allVideos
                    .filter {
                        it.player == playerName && it.dubbing == representative.dubbing
                    }
                    .dubbingEpisodeCount(),
                // Просмотры балансера — по всему тайтлу, а не по выбранной серии: порядок
                // в пикере не должен прыгать от серии к серии.
                views = allVideos.filter { it.player == playerName }.sumOf { it.views ?: 0 },
            )
        }
        .sortedWith(
            balancerDisplayComparator(
                playerName = { it.playerName },
                iframeUrl = { it.video.iframeUrl },
                views = { it.views },
            ),
        )
        .mapIndexed { index, option -> option.copy(baseIndex = index) }

    return if (options.isNotEmpty()) {
        DetailsPlayerSelection.ShowPicker(
            BalancerPickerState(
                episodeNumber = video.episode,
                options = options.toImmutableList(),
            ),
        )
    } else {
        DetailsPlayerSelection.Navigate(targetVideo)
    }
}
