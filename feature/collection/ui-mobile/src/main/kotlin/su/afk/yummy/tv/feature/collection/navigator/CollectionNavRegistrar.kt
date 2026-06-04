package su.afk.yummy.tv.feature.collection.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.collection.CollectionMobileScreen
import su.afk.yummy.tv.feature.collection.CollectionViewModel
import su.afk.yummy.tv.feature.collection.navigator.CollectionDestination
import javax.inject.Inject

class CollectionNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<CollectionDestination> { dest ->
                val viewModel = hiltViewModel<CollectionViewModel, CollectionViewModel.Factory>(
                    key = "mobile-collection-${dest.collectionId}",
                    creationCallback = { factory -> factory.create(dest.collectionId) },
                )
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    CollectionMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
