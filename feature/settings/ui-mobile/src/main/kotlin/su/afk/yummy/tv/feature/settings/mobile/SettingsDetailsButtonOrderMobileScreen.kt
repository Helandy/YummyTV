package su.afk.yummy.tv.feature.settings.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileDetailsButtonOrder
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsDetailsButtonOrderMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        SettingsDetailsButtonOrderMobileScreen(SettingsState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsDetailsButtonOrderMobileScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    val title = stringResource(R.string.settings_details_buttons_order)

    LaunchedEffect(Unit) {
        onEvent(SettingsState.Event.DetailsButtonOrderScreenOpened)
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = title,
                onBack = { onEvent(SettingsState.Event.BackSelected) },
            )
        },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_details)) {
                    SettingsMobileDetailsButtonOrder(
                        order = state.detailsButtonOrder,
                        onMove = { action, direction ->
                            onEvent(SettingsState.Event.DetailsButtonMoved(action, direction))
                        },
                        onReset = { onEvent(SettingsState.Event.DetailsButtonOrderReset) },
                    )
                }
            }
        }
    }
}
