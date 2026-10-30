package su.afk.yummy.tv.feature.details.details.model

import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary

/** Коллекции с тайтлом из одного запроса: бэйдж числа и первая страница для ленты в деталях. */
internal data class DetailsCollections(
    /** Бэйдж числа коллекций; `null`, если их нет или список не загрузился. */
    val badge: String? = null,
    val items: List<AnimeCollectionSummary> = emptyList(),
)
