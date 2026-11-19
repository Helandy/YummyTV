package su.afk.yummy.tv.feature.details.mobile.episodes.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.utils.kodik.isKodikSource
import su.afk.yummy.tv.feature.details.episodes.model.EpisodeRange
import su.afk.yummy.tv.feature.details.presentation.R

internal fun List<AnimeVideo>.representativeVideo(bestDubbing: String): AnimeVideo {
    val kodikVideos = filter { it.isKodikSource() }
    val source = kodikVideos.ifEmpty { this }
    return source.firstOrNull { bestDubbing.isNotBlank() && it.dubbing == bestDubbing }
        ?: source.maxByOrNull { it.views ?: 0 }
        ?: first()
}

/** Подпись чипа диапазона серий (`1–50`, либо одиночный номер на хвосте списка). */
@Composable
internal fun EpisodeRange.mobileChipLabel(): String =
    if (firstEpisode == lastEpisode) {
        firstEpisode
    } else {
        stringResource(R.string.details_episodes_range_label, firstEpisode, lastEpisode)
    }
