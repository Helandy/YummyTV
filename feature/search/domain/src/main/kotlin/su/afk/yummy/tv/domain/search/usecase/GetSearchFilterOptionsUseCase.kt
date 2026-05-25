package su.afk.yummy.tv.domain.search

/** Loads available filter values for anime search. */
class GetSearchFilterOptionsUseCase(private val repository: SearchRepository) {
    suspend operator fun invoke(): SearchFilterOptions = repository.getFilterOptions()
}
