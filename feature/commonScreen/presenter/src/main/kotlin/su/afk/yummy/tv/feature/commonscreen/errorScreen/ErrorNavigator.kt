package su.afk.yummy.tv.feature.commonscreen.errorScreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.EntryPointAccessors
import su.afk.yummy.tv.feature.commonscreen.di.ErrorNavigatorEntryPoint
import su.afk.yummy.tv.feature.commonscreen.navigator.CommonScreenDestination
import su.afk.yummy.tv.feature.commonscreen.navigator.IErrorNavigator
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import javax.inject.Inject

class ErrorNavigator @Inject constructor() : IErrorNavigator {
    override operator fun invoke(error: ErrorItem): NavKey = CommonScreenDestination.ErrorNavigatorDest(error = error)
}

class ErrorNavigatorRegister @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) = with(builder) {
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
                val savedStateHandle = extras.createSavedStateHandle()
                return assistedFactory.create(dest, savedStateHandle) as T
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
