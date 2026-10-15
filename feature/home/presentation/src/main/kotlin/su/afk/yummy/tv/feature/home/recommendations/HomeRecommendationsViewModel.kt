package su.afk.yummy.tv.feature.home.recommendations

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.HomeAnalytics
import javax.inject.Inject

/**
 * Показывает все рекомендации из кешированной главной ленты. Скрытые пользователем тайтлы
 * убираются и на лету, если скрытие изменилось.
 */
@HiltViewModel
class HomeRecommendationsViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase,
    private val settingsStore: SettingsStore,
    private val analytics: HomeAnalytics,
) : BaseViewModel<HomeRecommendationsState.State, HomeRecommendationsState.Event, HomeRecommendationsState.Effect>() {

    override fun createInitialState() = HomeRecommendationsState.State()

    init {
        viewModelScope.launch {
            val section = getCachedHomeFeed()?.sections
                ?.firstOrNull { it.type == HomeFeedSectionType.RECOMMENDATIONS }
            setState { copy(title = section?.title.orEmpty()) }
            settingsStore.hiddenRecommendationIds
                .onEach { hiddenIds ->
                    val visible = section?.items.orEmpty().filterNot { it.id in hiddenIds }
                    setState { copy(items = visible.toImmutableList(), isLoading = false) }
                }
                .launchIn(this)
        }
    }

    override fun onEvent(event: HomeRecommendationsState.Event) {
        when (event) {
            HomeRecommendationsState.Event.BackSelected -> nav.back()
            is HomeRecommendationsState.Event.AnimeSelected -> {
                analytics.eventAnimeSelected(event.animeId)
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }
        }
    }
}
