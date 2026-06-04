package su.afk.yummy.tv.feature.schedule.mobile.navigator

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.schedule.ScheduleMobileScreen
import su.afk.yummy.tv.feature.schedule.ScheduleViewModel
import su.afk.yummy.tv.feature.schedule.navigator.ScheduleDestination
import javax.inject.Inject

class ScheduleNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<ScheduleDestination> {
                val viewModel = hiltViewModel<ScheduleViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    ScheduleMobileScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}
