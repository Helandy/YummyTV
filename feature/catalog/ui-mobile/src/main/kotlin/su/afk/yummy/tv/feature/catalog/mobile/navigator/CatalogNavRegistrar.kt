package su.afk.yummy.tv.feature.catalog.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.catalog.IMobileCatalogEntry
import su.afk.yummy.tv.feature.catalog.CatalogViewModel
import su.afk.yummy.tv.feature.catalog.mobile.CatalogMobileScreen
import su.afk.yummy.tv.feature.catalog.navigator.CatalogDestination
import javax.inject.Inject

class CatalogNavRegistrar @Inject constructor() : IMobileCatalogEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<CatalogDestination> { destination ->
                val viewModel = hiltViewModel<CatalogViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    CatalogMobileScreen(
                        dest = destination,
                        state = state,
                        effect = effect,
                        onEvent = onEvent
                    )
                }
            }
        }
}
