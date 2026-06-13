package su.afk.yummy.tv.feature.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.domain.search.model.SearchFilters
import su.afk.yummy.tv.domain.search.model.SearchPage
import su.afk.yummy.tv.domain.search.usecase.SearchUseCase
import javax.inject.Inject

internal class SearchPagingHandler @Inject constructor(
    private val search: SearchUseCase,
) {
    private var searchJob: Job? = null

    fun cancel() {
        searchJob?.cancel()
        searchJob = null
    }

    fun canLoadMore(
        query: String,
        filters: SearchFilters,
        isLoading: Boolean,
        canLoadMore: Boolean,
    ): Boolean =
        !isLoading && canLoadMore && (query.isNotBlank() || !filters.isEmpty)

    fun normalizedYears(filters: SearchFilters): SearchFilters {
        val from = filters.fromYear
        val to = filters.toYear
        return if (from != null && to != null && from > to) {
            filters.copy(fromYear = to, toYear = from)
        } else {
            filters
        }
    }

    fun load(
        scope: CoroutineScope,
        request: SearchPagingRequest,
        debounceMs: Long = 0L,
        onLoading: (SearchPagingRequest) -> Unit,
        onResult: (SearchPagingResult) -> Unit,
    ) {
        searchJob?.cancel()
        searchJob = scope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            onLoading(request)
            runCatching {
                loadVisiblePage(
                    query = request.query,
                    filters = request.filters,
                    offset = request.offset,
                )
            }.fold(
                onSuccess = { page -> onResult(SearchPagingResult.Success(request, page)) },
                onFailure = { error -> onResult(SearchPagingResult.Failure(request, error)) },
            )
        }
    }

    private suspend fun loadVisiblePage(
        query: String,
        filters: SearchFilters,
        offset: Int,
    ): SearchPage {
        var page = search(query, filters, PAGE_SIZE, offset)
        while (page.items.isEmpty() && page.canLoadMore && page.nextOffset > offset) {
            val nextPage = search(query, filters, PAGE_SIZE, page.nextOffset)
            page = page.copy(
                items = nextPage.items,
                nextOffset = nextPage.nextOffset,
                canLoadMore = nextPage.canLoadMore,
            )
        }
        return page
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}

internal data class SearchPagingRequest(
    val query: String,
    val filters: SearchFilters,
    val offset: Int,
    val replace: Boolean,
)

internal sealed interface SearchPagingResult {
    val request: SearchPagingRequest

    data class Success(
        override val request: SearchPagingRequest,
        val page: SearchPage,
    ) : SearchPagingResult

    data class Failure(
        override val request: SearchPagingRequest,
        val error: Throwable,
    ) : SearchPagingResult
}
