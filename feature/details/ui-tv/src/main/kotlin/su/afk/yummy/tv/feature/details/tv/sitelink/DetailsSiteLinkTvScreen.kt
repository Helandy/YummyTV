package su.afk.yummy.tv.feature.details.tv.sitelink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.tv.TvStateMessage
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.sitelink.DetailsSiteLinkState

/** Лоадер на время разрешения ссылки сайта; затем экран заменяется деталями тайтла. */
@Composable
fun DetailsSiteLinkTvScreen(
    state: DetailsSiteLinkState.State,
    effect: Flow<DetailsSiteLinkState.Effect>,
    onEvent: (DetailsSiteLinkState.Event) -> Unit,
) {
    val retryFocusRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        if (state.isFailed) {
            TvStateMessage(
                title = stringResource(R.string.details_site_link_error),
                icon = Icons.Filled.Warning,
                onRetry = { onEvent(DetailsSiteLinkState.Event.RetrySelected) },
                retryFocusRequester = retryFocusRequester,
            )
            LaunchedEffect(Unit) { requestFocusUntilTimeout(retryFocusRequester) }
        } else {
            YummyLoadingIndicator()
        }
    }
}
