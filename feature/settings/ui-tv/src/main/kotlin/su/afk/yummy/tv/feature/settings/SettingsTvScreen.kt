package su.afk.yummy.tv.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.utils.restartApplication
import su.afk.yummy.tv.feature.settings.model.SettingsTab
import su.afk.yummy.tv.feature.settings.utils.label
import su.afk.yummy.tv.feature.settings.view.SettingsTvPanelHost
import su.afk.yummy.tv.feature.settings.view.SettingsTvTabRow
import su.afk.yummy.tv.feature.settings.view.TvInterfaceModeConfirmationDialog

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true
)
@Composable
private fun SettingsTvScreenDefaultPreview() = ScreenPreviewTheme {
    SettingsTvScreen(SettingsState.State(), emptyFlow()) {}
}

@Composable
fun SettingsTvScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(SettingsTab.THEME) }
    var pendingInterfaceMode by remember { mutableStateOf<AppInterfaceMode?>(null) }
    val context = LocalContext.current
    val contentFocusRequesters = remember {
        SettingsTab.entries.associateWith { FocusRequester() }
    }
    val tabFocusRequesters = remember {
        SettingsTab.entries.associateWith { FocusRequester() }
    }
    val selectedTabFocusRequester = tabFocusRequesters.getValue(selectedTab)
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current

    LaunchedEffect(effect) {
        effect.collect { settingsEffect ->
            if (settingsEffect is SettingsState.Effect.RestartApplication &&
                !context.restartApplication()
            ) {
                Toast.makeText(
                    context,
                    R.string.settings_interface_restart_failed,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    DisposableEffect(selectedTabFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(selectedTabFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = TvScreenPadding.Horizontal, vertical = TvScreenPadding.Vertical),
    ) {
        SettingsTvTabRow(
            selectedTab = selectedTab,
            tabFocusRequesters = tabFocusRequesters,
            contentFocusRequesters = contentFocusRequesters,
            mainMenuFocusRequester = mainMenuFocusRequester,
            onSelectedTabChanged = { selectedTab = it },
        )

        Spacer(modifier = Modifier.height(32.dp))

        SettingsTvPanelHost(
            state = state,
            selectedTab = selectedTab,
            tabFocusRequesters = tabFocusRequesters,
            contentFocusRequesters = contentFocusRequesters,
            onInterfaceModeSelected = { selectedMode ->
                if (selectedMode != state.interfaceMode) {
                    pendingInterfaceMode = selectedMode
                }
            },
            onEvent = onEvent,
            modifier = Modifier.weight(1f),
        )
    }

    pendingInterfaceMode?.let { targetMode ->
        TvInterfaceModeConfirmationDialog(
            targetModeLabel = targetMode.label(),
            onConfirm = {
                pendingInterfaceMode = null
                onEvent(SettingsState.Event.InterfaceModeSelected(targetMode))
            },
            onDismiss = { pendingInterfaceMode = null },
        )
    }
}
