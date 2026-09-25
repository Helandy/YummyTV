package su.afk.yummy.tv.feature.commonscreen.errorScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.EntryPointAccessors
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.error.api.ErrorDestinationFactory
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.commonscreen.di.ErrorNavigatorEntryPoint
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
                ErrorNavigatorEntry(dest)
            }
        }
}

@Composable
private fun ErrorNavigatorEntry(dest: CommonScreenDestination.ErrorNavigatorDest) {
    val appContext = LocalContext.current.applicationContext

    val entryPoint = EntryPointAccessors.fromApplication(
        appContext,
        ErrorNavigatorEntryPoint::class.java
    )
    val assistedFactory = entryPoint.creatorErrorViewModelFactory()

    val vm: ErrorViewModel = viewModel(
        key = "ErrorNavigatorDest:${dest}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                return assistedFactory.create(dest) as T
            }
        }
    )

    ScreenNavigator(vm) { state, effect, event ->
        ErrorScreen(
            state = state,
            onEvent = event,
            effect = effect,
        )
    }
}
