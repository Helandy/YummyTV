package su.afk.yummy.tv.feature.settings.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileLibraryTabOrder
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.presentation.R

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsLibraryTabOrderMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        SettingsLibraryTabOrderMobileScreen(SettingsState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsLibraryTabOrderMobileScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    val title = stringResource(R.string.settings_library_tabs_order)

    LaunchedEffect(Unit) {
        onEvent(SettingsState.Event.LibraryTabOrderScreenOpened)
    }

    BaseScreen(
        isScroll = false,
        contentModifier = Modifier.navigationBarsPadding(),
        customTopBar = {
            MobileTopBar(
                title = title,
                onBack = { onEvent(SettingsState.Event.BackSelected) },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.mobileContentMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                SettingsMobileSection(title = title) {
                    SettingsMobileLibraryTabOrder(
                        order = state.libraryTabOrder,
                        onMove = { tab, direction ->
                            onEvent(SettingsState.Event.LibraryTabMoved(tab, direction))
                        },
                        onReset = { onEvent(SettingsState.Event.LibraryTabOrderReset) },
                    )
                }
            }
        }
    }
}
