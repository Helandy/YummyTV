package su.afk.yummy.tv.domain.search

private const val BLOCKED_REGION = "RU"

class SearchUseCase(
    private val repository: SearchRepository,
    private val hideRegionBlocked: Boolean,
) {
    suspend operator fun invoke(
        query: String,
        filters: SearchFilters = SearchFilters.EMPTY,
        limit: Int = 40,
        offset: Int = 0,
    ): SearchPage {
        val page = repository.search(query, filters, limit, offset)
        return if (!hideRegionBlocked) {
            page
        } else {
            page.copy(items = page.items.filterNot { it.isBlockedInRegion() })
        }
    }

    private fun SearchItem.isBlockedInRegion(): Boolean =
        blockedIn.any { it.equals(BLOCKED_REGION, ignoreCase = true) }
}

class GetSearchFilterOptionsUseCase(private val repository: SearchRepository) {
    suspend operator fun invoke(): SearchFilterOptions = repository.getFilterOptions()
}
