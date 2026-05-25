package su.afk.yummy.tv.domain.search.usecase

import su.afk.yummy.tv.domain.search.model.SearchFilterOptions
import su.afk.yummy.tv.domain.search.repository.SearchRepository

/** Loads available filter values for anime search. */
class GetSearchFilterOptionsUseCase(private val repository: SearchRepository) {
    suspend operator fun invoke(): SearchFilterOptions = repository.getFilterOptions()
}
