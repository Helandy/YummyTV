package su.afk.yummy.tv.core.update.nav

import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.update.UpdateState
import su.afk.yummy.tv.core.update.UpdateViewModel
import javax.inject.Inject

class UpdateNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<UpdateDestination> { dest ->
                val viewModel = hiltViewModel<UpdateViewModel>()

                LaunchedEffect(dest) {
                    viewModel.initWithUpdateInfo(
                        dest.version,
                        dest.apkUrl,
                        dest.changelog,
                        dest.required
                    )
                }

                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    LaunchedEffect(Unit) {
                        effect.collect { if (it is UpdateState.Effect.NavigateBack) nav.back() }
                    }
                    UpdateDialog(status = state.status, onEvent = onEvent)
                }
            }
        }
}
