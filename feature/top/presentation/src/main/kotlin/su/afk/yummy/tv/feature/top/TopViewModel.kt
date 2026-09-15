package su.afk.yummy.tv.feature.top

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.core.utils.paging.OffsetPage
import su.afk.yummy.tv.core.utils.paging.OffsetPagingSource
import su.afk.yummy.tv.domain.top.model.AnimeTopItem
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.domain.top.usecase.GetAnimeTopUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

@HiltViewModel
class TopViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAnimeTop: GetAnimeTopUseCase,
    settingsStore: AppearanceSettingsStore,
    private val analytics: TopAnalytics,
) : BaseViewModel<TopState.State, TopState.Event, TopState.Effect>() {

    override fun createInitialState() = TopState.State(items = createPagingFlow(AnimeTopType.TV))

    private companion object {
        const val PAGE_SIZE = 100
    }

    init {
        analytics.eventScreenOpened()
        settingsStore.showTopTitleYear
            .onEach { showTitleYear -> setState { copy(showTitleYear = showTitleYear) } }
            .launchIn(viewModelScope)
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
                OffsetPagingSource(itemKey = { it.id }) { limit, offset ->
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
