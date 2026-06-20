package su.afk.yummy.tv.feature.top

import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.utils.OffsetPage
import su.afk.yummy.tv.core.utils.OffsetPagingSource
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.domain.top.usecase.GetAnimeTopUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

@HiltViewModel
class TopViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeTop: GetAnimeTopUseCase,
    private val analytics: TopAnalytics,
) : BaseViewModelNew<TopState.State, TopState.Event, TopState.Effect>(savedStateHandle) {

    override fun createInitialState() = TopState.State(items = createPagingFlow(AnimeTopType.TV))

    private companion object {
        const val PAGE_SIZE = 100
    }

    init {
        analytics.eventScreenOpened()
    }

    override fun onEvent(event: TopState.Event) {
        when (event) {
            is TopState.Event.TypeSelected -> {
                if (event.type != currentState.selectedType) {
                    analytics.eventTypeSelected(event.type)
                    setState {
                        copy(
                            selectedType = event.type,
                            items = createPagingFlow(event.type),
                        )
                    }
                }
            }

            is TopState.Event.AnimeSelected -> {
                analytics.eventAnimeSelected(event.animeId)
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }

            TopState.Event.RetrySelected -> analytics.eventRetry()
        }
    }

    private fun createPagingFlow(type: AnimeTopType) =
        Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                OffsetPagingSource { limit, offset ->
                    loadTopPage(type, limit, offset)
                }
            },
        ).flow.cachedIn(viewModelScope)

    private suspend fun loadTopPage(
        type: AnimeTopType,
        limit: Int,
        offset: Int,
    ): OffsetPage<AnimeTopItem> =
        runCatching {
            getAnimeTop(type, limit, offset)
        }.fold(
            onSuccess = { page ->
                OffsetPage(
                    items = page.items,
                    nextOffset = page.nextOffset,
                    canLoadMore = page.canLoadMore,
                )
            },
            onFailure = { error ->
                analytics.eventLoadError(type, error)
                throw error
            },
        )
}
