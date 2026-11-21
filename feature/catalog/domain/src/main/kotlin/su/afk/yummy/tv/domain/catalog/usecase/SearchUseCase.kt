package su.afk.yummy.tv.domain.catalog.usecase

import su.afk.yummy.tv.domain.catalog.model.SearchFilters
import su.afk.yummy.tv.domain.catalog.model.SearchPage
import su.afk.yummy.tv.domain.catalog.repository.SearchRepository
import javax.inject.Inject

/**
 * Ищет аниме по запросу, фильтрам и параметрам страницы.
 * По умолчанию результаты отсортированы по топу, как в каталоге на сайте, в том числе при поиске по тексту.
При [forceRefresh] кэш пропускается и страница берётся из сети (кэш остаётся запасным при ошибке).
 */
class SearchUseCase @Inject constructor(private val repository: SearchRepository) {
    suspend operator fun invoke(
        query: String,
        filters: SearchFilters = SearchFilters.EMPTY,
        limit: Int = 40,
        offset: Int = 0,
        forceRefresh: Boolean = false,
    ): SearchPage = repository.search(query, filters, limit, offset, forceRefresh)
}
