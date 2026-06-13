package su.afk.yummy.tv.feature.search.tv.navigator

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.search.SearchState
import su.afk.yummy.tv.feature.search.SearchTvScreen
import su.afk.yummy.tv.feature.search.SearchViewModel
import su.afk.yummy.tv.feature.search.navigator.SearchDestination
import javax.inject.Inject

class SearchNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<SearchDestination> { destination ->
                val viewModel = hiltViewModel<SearchViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    LaunchedEffect(destination.initialQuery) {
                        if (destination.initialQuery.isNotBlank()) {
                            onEvent(SearchState.Event.ExternalSearchSubmitted(destination.initialQuery))
                        }
                    }
                    SearchTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
