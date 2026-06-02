package su.afk.yummy.tv.feature.schedule.navigator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.ScreenNavigator
import su.afk.yummy.tv.core.navigation.NavRegistrar
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.utils.PlatformCapabilities
import su.afk.yummy.tv.feature.schedule.R
import su.afk.yummy.tv.feature.schedule.ScheduleTvScreen
import su.afk.yummy.tv.feature.schedule.ScheduleViewModel
import javax.inject.Inject

class ScheduleNavRegistrar @Inject constructor() : NavRegistrar {
    override fun register(builder: EntryProviderScope<NavKey>, nav: NavigationManager) =
        with(builder) {
            entry<ScheduleDestination> {
                if (!PlatformCapabilities.supportsSchedule) {
                    ScheduleUnsupportedScreen()
                    return@entry
                }

                val viewModel = hiltViewModel<ScheduleViewModel>()
                ScreenNavigator(viewModel) { state, effect, onEvent ->
                    ScheduleTvScreen(state = state, effect = effect, onEvent = onEvent)
                }
            }
        }
}

@Composable
private fun ScheduleUnsupportedScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.schedule_unsupported_android7),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
