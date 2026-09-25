package su.afk.yummy.tv.feature.watchlater.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.watchlater.WatchLaterState
import su.afk.yummy.tv.feature.watchlater.mobile.view.WatchLaterMobileCard

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun WatchLaterMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        WatchLaterMobileScreen(WatchLaterState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WatchLaterMobileScreen(
    state: WatchLaterState.State,
    effect: Flow<WatchLaterState.Effect>,
    onEvent: (WatchLaterState.Event) -> Unit,
) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.watch_later_title),
                onBack = { onEvent(WatchLaterState.Event.BackSelected) },
            )
        },
    ) {
        if (state.items.isEmpty()) {
            MobileMessage(
                title = stringResource(R.string.watch_later_empty),
                icon = Icons.Outlined.BookmarkBorder,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .mobileContentMaxWidth()
                    .fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = MobileBottomBarDefaults.contentBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { "${it.animeId}_${it.episode}" }) { item ->
                    WatchLaterMobileCard(
                        item = item,
                        onClick = {
                            onEvent(
                                WatchLaterState.Event.ItemSelected(item.animeId, item.episode)
                            )
                        },
                        onRemove = {
                            onEvent(
                                WatchLaterState.Event.RemoveSelected(item.animeId, item.episode)
                            )
                        },
                    )
                }
            }
        }
    }
}
