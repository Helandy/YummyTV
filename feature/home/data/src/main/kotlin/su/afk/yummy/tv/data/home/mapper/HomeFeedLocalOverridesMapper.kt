package su.afk.yummy.tv.data.home.mapper

import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.data.home.storage.mapper.toHomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType

internal class LocalFeedContext(
    val languageCode: String,
    val watchEntries: List<WatchProgressEntry>,
    val hiddenIds: Set<Int>,
)

// Применяется одинаково к результату из кэша, из сети и к fallback при ошибке: "продолжить
// просмотр" всегда пересчитывается из актуального локального прогресса, а не из момента
// кэширования фида, а скрытые пользователем рекомендации отфильтровываются до ближайшего
// пересчёта рекомендаций на бэкенде.
internal fun HomeFeed.withLocalOverrides(
    localEntries: List<WatchProgressEntry>,
    hiddenIds: Set<Int>,
): HomeFeed = copy(
    continueWatchingItems = localContinueWatchingItems(localEntries),
    sections = if (hiddenIds.isEmpty()) {
        sections
    } else {
        sections.map { section ->
            if (section.type == HomeFeedSectionType.RECOMMENDATIONS) {
                section.copy(items = section.items.filterNot { it.id in hiddenIds })
            } else {
                section
            }
        }
    },
)

internal fun localContinueWatchingItems(
    entries: List<WatchProgressEntry>,
): List<HomeContinueWatchingItem> =
    entries
        .filter { it.animeId > 0 }
        .groupBy { it.animeId }
        .values
        .mapNotNull { group ->
            group.maxWithOrNull(
                compareBy<WatchProgressEntry> { it.updatedAt }
                    .thenBy { it.positionMs }
                    .thenBy { it.videoId }
                    .thenBy { it.episode },
            )
        }
        .sortedByDescending { it.updatedAt }
        .map { it.toHomeContinueWatchingItem() }
