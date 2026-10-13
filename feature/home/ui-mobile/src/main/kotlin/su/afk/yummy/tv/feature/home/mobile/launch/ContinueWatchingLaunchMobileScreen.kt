package su.afk.yummy.tv.feature.home.mobile.launch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.feature.home.launch.ContinueWatchingLaunchState

/** Лоадер на время разрешения запуска «продолжить просмотр»; затем экран заменяется плеером. */
@Composable
fun ContinueWatchingLaunchMobileScreen(
    state: ContinueWatchingLaunchState.State,
    effect: Flow<ContinueWatchingLaunchState.Effect>,
    onEvent: (ContinueWatchingLaunchState.Event) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
