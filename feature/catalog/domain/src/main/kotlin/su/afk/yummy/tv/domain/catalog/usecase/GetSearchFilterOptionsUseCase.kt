package su.afk.yummy.tv.domain.catalog.usecase

import su.afk.yummy.tv.domain.catalog.model.SearchFilterOptions
import su.afk.yummy.tv.domain.catalog.repository.SearchRepository
import javax.inject.Inject

/** Загружает доступные значения фильтров для поиска аниме. */
class GetSearchFilterOptionsUseCase @Inject constructor(private val repository: SearchRepository) {
    suspend operator fun invoke(): SearchFilterOptions = repository.getFilterOptions()
}
