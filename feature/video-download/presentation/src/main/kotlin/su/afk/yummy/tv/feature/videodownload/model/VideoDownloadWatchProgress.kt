package su.afk.yummy.tv.feature.videodownload.model

import androidx.compose.runtime.Immutable

/** Прогресс просмотра скачанной серии для карточки в списке загрузок. */
@Immutable
data class VideoDownloadWatchProgress(
    val positionMs: Long,
    val durationMs: Long,
    /** Серия досмотрена по пользовательскому порогу «просмотрено». */
    val isWatched: Boolean,
) {
    /** Доля просмотра для полоски под превью; у досмотренной серии — полная. */
    val fraction: Float
        get() = if (isWatched) 1f else (positionMs.toFloat() / durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
}
