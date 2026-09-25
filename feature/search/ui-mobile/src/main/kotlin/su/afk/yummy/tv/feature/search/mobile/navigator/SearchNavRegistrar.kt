package su.afk.yummy.tv.feature.search.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.search.IMobileSearchEntry
import su.afk.yummy.tv.feature.search.SearchViewModel
import su.afk.yummy.tv.feature.search.mobile.SearchMobileScreen
import su.afk.yummy.tv.feature.search.navigator.SearchDestination
import javax.inject.Inject

class SearchNavRegistrar @Inject constructor() : IMobileSearchEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<SearchDestination> { destination ->
                val viewModel = hiltViewModel<SearchViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    SearchMobileScreen(
                        dest = destination,
                        state = state,
                        effect = effect,
                        onEvent = onEvent
                    )
                }
            }
        }
}
