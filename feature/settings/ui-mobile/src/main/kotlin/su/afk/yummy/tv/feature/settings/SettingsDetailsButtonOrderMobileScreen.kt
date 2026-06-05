package su.afk.yummy.tv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.view.SettingsMobileDetailsButtonOrder
import su.afk.yummy.tv.feature.settings.view.SettingsMobileSection

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsDetailsButtonOrderMobileScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
    onBack: () -> Unit,
) {
    val title = stringResource(R.string.settings_details_buttons_order)

    BaseScreen(
        isScroll = false,
        customTopBar = { MobileTopBar(title = title, onBack = onBack) },
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
