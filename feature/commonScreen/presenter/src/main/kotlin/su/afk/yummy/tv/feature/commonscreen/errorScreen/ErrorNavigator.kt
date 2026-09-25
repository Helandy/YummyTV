package su.afk.yummy.tv.feature.commonscreen.errorScreen

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.error.api.ErrorDestinationFactory
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.commonscreen.navigator.CommonScreenDestination
import su.afk.yummy.tv.feature.commonscreen.navigator.IErrorScreenEntry
import javax.inject.Inject

class ErrorNavigator @Inject constructor() : ErrorDestinationFactory {
    override operator fun invoke(error: ErrorItem): NavKey =
        CommonScreenDestination.ErrorNavigatorDest(error = error)
}

class ErrorNavigatorRegister @Inject constructor() : IErrorScreenEntry {
    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<CommonScreenDestination.ErrorNavigatorDest> { dest ->
                val vm = hiltViewModel<ErrorViewModel, ErrorViewModel.Factory>(
                    key = "ErrorNavigatorDest:$dest",
                ) { it.create(dest) }
                ScreenNavigator(vm) { state, effect, onEvent ->
                    ErrorScreen(state = state, onEvent = onEvent, effect = effect)
                }
            }
        }
}
