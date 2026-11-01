package su.afk.yummy.tv.feature.videodownload.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.feature.videodownload.utils.isInProgress

/** Сериал со всеми его скачанными сериями: строка списка, раскрываемая в серии. */
@Immutable
data class VideoDownloadGroup(
    /** Стабильный ключ группы: `anime:<id>` или `title:<название>` для тайтлов без id. */
    val key: String,
    val animeId: Int,
    val title: String,
    val posterUrl: String,
    val episodes: ImmutableList<VideoDownloadItem>,
) {
    val episodeCount: Int
        get() = episodes.size

    /** Суммарный объём скачанных данных по сериям группы. */
    val occupiedBytes: Long
        get() = episodes.sumOf { it.bytesDownloaded.coerceAtLeast(0L) }

    /** Сколько серий сейчас в очереди, качается или выгружается. */
    val activeCount: Int
        get() = episodes.count { it.isInProgress }
}
