package su.afk.yummy.tv.feature.details.mobile.sitelink

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.sitelink.DetailsSiteLinkState

/** Лоадер на время разрешения ссылки сайта; затем экран заменяется деталями тайтла. */
@Composable
fun DetailsSiteLinkMobileScreen(
    state: DetailsSiteLinkState.State,
    effect: Flow<DetailsSiteLinkState.Effect>,
    onEvent: (DetailsSiteLinkState.Event) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        if (state.isFailed) {
            MobileTopBar(
                title = "",
                onBack = { onEvent(DetailsSiteLinkState.Event.BackSelected) },
                modifier = Modifier.align(Alignment.TopStart),
            )
            MobileMessage(
                title = stringResource(R.string.details_site_link_error),
                icon = Icons.Filled.Warning,
                actionLabel = stringResource(CoreR.string.common_retry),
                onAction = { onEvent(DetailsSiteLinkState.Event.RetrySelected) },
            )
        } else {
            YummyLoadingIndicator()
        }
    }
}
