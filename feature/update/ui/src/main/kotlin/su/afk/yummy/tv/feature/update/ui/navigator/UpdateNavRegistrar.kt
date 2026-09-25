package su.afk.yummy.tv.feature.update.ui.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.baseScreen.ScreenNavigator
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.update.UpdateViewModel
import su.afk.yummy.tv.feature.update.navigator.IUpdateEntry
import su.afk.yummy.tv.feature.update.navigator.UpdateDestination
import su.afk.yummy.tv.feature.update.view.UpdateDialog
import javax.inject.Inject

/** Регистрирует диалог обновления; экран общий для mobile и TV. */
class UpdateNavRegistrar @Inject constructor() : IUpdateEntry {

    override fun register(builder: EntryProviderScope<NavKey>, nav: INavigationManager) =
        with(builder) {
            entry<UpdateDestination> { dest ->
                val viewModel = hiltViewModel<UpdateViewModel>()

                ScreenNavigator(viewModel) { state, _, onEvent ->
                    UpdateDialog(dest = dest, status = state.status, onEvent = onEvent)
                }
            }
        }
}
