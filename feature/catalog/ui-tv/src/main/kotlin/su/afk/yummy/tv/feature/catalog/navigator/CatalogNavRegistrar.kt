package su.afk.yummy.tv.feature.catalog.tv.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.catalog.ITvCatalogEntry
import su.afk.yummy.tv.feature.catalog.CatalogTvScreen
import su.afk.yummy.tv.feature.catalog.CatalogViewModel
import su.afk.yummy.tv.feature.catalog.navigator.CatalogDestination
import javax.inject.Inject

class CatalogNavRegistrar @Inject constructor() : ITvCatalogEntry {

    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<CatalogDestination> { destination ->
                val viewModel = hiltViewModel<CatalogViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    CatalogTvScreen(
                        dest = destination,
                        state = state,
                        effect = effect,
                        onEvent = onEvent
                    )
                }
            }
        }
}
