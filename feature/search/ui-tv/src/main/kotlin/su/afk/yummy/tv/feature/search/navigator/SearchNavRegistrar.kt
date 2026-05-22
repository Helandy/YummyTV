package su.afk.yummy.tv.feature.search.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.search.SearchTvScreen
import su.afk.yummy.tv.feature.search.SearchViewModel
import javax.inject.Inject

class SearchNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<SearchDestination> { _ ->
                val viewModel = hiltViewModel<SearchViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SearchTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
