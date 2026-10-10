package su.afk.yummy.tv.feature.library.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.utils.episode.episodeGroupKey
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.LibraryPoster
import su.afk.yummy.tv.domain.library.model.WatchHistoryEntry
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.model.LibraryTab

internal fun LibraryState.State.tvTabItemCount(tab: LibraryTab): Int? = when (tab) {
    LibraryTab.CONTINUE_WATCHING -> continueWatching.size
    LibraryTab.HISTORY -> null
    else -> tabItems[tab]?.size ?: 0
}

internal fun LibraryTab.focusStateKey(source: String): String = "${name}_$source"

@Composable
internal fun LibraryTab.tabColor(): Color = when (this) {
    LibraryTab.CONTINUE_WATCHING -> MaterialTheme.colorScheme.primary
    LibraryTab.HISTORY -> MaterialTheme.colorScheme.tertiary
    else -> requireNotNull(semanticColorOrNull())
}

internal fun HomeContinueWatchingItem.continueWatchingFocusKey(): String =
    "$animeId:$videoId:$episode:$episodeUrl"

/**
 * Ключи восстановления фокуса в истории.
 *
 * Намеренно без watchedAtSeconds: после просмотра серии сервер обновляет время, запись уезжает
 * вверх списка, и фокус должен приехать за ней. Но пара animeId+серия в истории не уникальна
 * (пересмотр, разные озвучки), поэтому к повторам дописывается порядковый номер — иначе строки
 * делят один FocusRequester и requestFocus() уводит на первую из них, то есть на карточку выше.
 */
internal fun List<WatchHistoryEntry>.historyFocusKeys(): List<String> {
    val seen = HashMap<String, Int>()
    return map { entry ->
        val base = "${entry.animeId}:${entry.episode.episodeGroupKey()}"
        val ordinal = seen.merge(base, 1, Int::plus)!! - 1
        if (ordinal == 0) base else "$base#$ordinal"
    }
}

internal fun LibraryItem.posterUrl(quality: PosterQuality): String? = poster.posterUrl(quality)

private fun LibraryPoster?.posterUrl(quality: PosterQuality): String? = when (quality) {
    PosterQuality.LOW -> this?.medium ?: this?.big ?: this?.fullsize ?: this?.small
    PosterQuality.STANDARD -> this?.big ?: this?.medium ?: this?.fullsize ?: this?.small
    PosterQuality.MEGA -> this?.mega ?: this?.big ?: this?.medium ?: this?.fullsize ?: this?.small
    PosterQuality.HIGH -> this?.fullsize ?: this?.mega ?: this?.big ?: this?.medium ?: this?.small
}
