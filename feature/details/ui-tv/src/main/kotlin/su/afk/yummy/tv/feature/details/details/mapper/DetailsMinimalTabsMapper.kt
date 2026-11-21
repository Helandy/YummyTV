package su.afk.yummy.tv.feature.details.details.mapper

import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.model.settings.DetailsSectionKind
import su.afk.yummy.tv.feature.details.details.model.DetailsMinimalTab

/**
 * Вкладки упрощённой деталки: «Серии» первыми, дальше блоки деталки в порядке из настроек.
 * Блок без данных вкладки не получает — пустой экран по фокусу не нужен.
 */
internal fun List<DetailsSectionKind>.toDetailsMinimalTabs(
    hasScreenshots: Boolean,
    hasVideo: Boolean,
    hasUniverse: Boolean,
    hasCollections: Boolean,
    hasReviews: Boolean,
): List<DetailsMinimalTab> = buildList {
    add(DetailsMinimalTab.EPISODES)
    this@toDetailsMinimalTabs.forEach { section ->
        val tab = when (section) {
            DetailsSectionKind.INFO, DetailsSectionKind.DESCRIPTION -> DetailsMinimalTab.INFO
            DetailsSectionKind.SCREENSHOTS -> DetailsMinimalTab.SCREENSHOTS.takeIf { hasScreenshots }
            DetailsSectionKind.VIDEO -> DetailsMinimalTab.VIDEO.takeIf { hasVideo }
            DetailsSectionKind.RATING -> DetailsMinimalTab.RATING
            DetailsSectionKind.UNIVERSE -> DetailsMinimalTab.RELATED.takeIf { hasUniverse }
            DetailsSectionKind.SIMILAR -> DetailsMinimalTab.SIMILAR
            DetailsSectionKind.COLLECTIONS -> DetailsMinimalTab.COLLECTIONS.takeIf { hasCollections }
            DetailsSectionKind.REVIEWS -> DetailsMinimalTab.REVIEWS.takeIf { hasReviews }
            DetailsSectionKind.COMMENTS -> DetailsMinimalTab.COMMENTS
        }
        if (tab != null && tab !in this) add(tab)
    }
}

/** Кнопка панели, которую заменяет вкладка: её содержимое уже на экране, дублировать не нужно. */
internal val DetailsMinimalTab.replacedAction: DetailsButtonAction
    get() = when (this) {
        DetailsMinimalTab.EPISODES -> DetailsButtonAction.EPISODES
        DetailsMinimalTab.INFO -> DetailsButtonAction.FULL_DETAILS
        DetailsMinimalTab.SCREENSHOTS -> DetailsButtonAction.SCREENSHOTS
        DetailsMinimalTab.VIDEO -> DetailsButtonAction.TRAILERS
        DetailsMinimalTab.RATING -> DetailsButtonAction.RATING
        DetailsMinimalTab.RELATED -> DetailsButtonAction.VIEWING_ORDER
        DetailsMinimalTab.SIMILAR -> DetailsButtonAction.SIMILAR
        DetailsMinimalTab.COLLECTIONS -> DetailsButtonAction.COLLECTIONS
        DetailsMinimalTab.REVIEWS -> DetailsButtonAction.REVIEWS
        DetailsMinimalTab.COMMENTS -> DetailsButtonAction.COMMENTS
    }
