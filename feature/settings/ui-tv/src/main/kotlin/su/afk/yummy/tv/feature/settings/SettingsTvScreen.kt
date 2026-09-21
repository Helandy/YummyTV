package su.afk.yummy.tv.feature.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.utils.system.restartApplication
import su.afk.yummy.tv.feature.settings.model.SettingsTab
import su.afk.yummy.tv.feature.settings.model.SettingsTvPicker
import su.afk.yummy.tv.feature.settings.utils.color
import su.afk.yummy.tv.feature.settings.utils.label
import su.afk.yummy.tv.feature.settings.view.SettingsTvCategoryList
import su.afk.yummy.tv.feature.settings.view.SettingsTvPanelHost
import su.afk.yummy.tv.feature.settings.view.SettingsTvPickerPanel
import su.afk.yummy.tv.feature.settings.view.SettingsTvValueRow
import su.afk.yummy.tv.feature.settings.view.TvInterfaceModeConfirmationDialog
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvPickerRow
import su.afk.yummy.tv.feature.settings.view.valueText

@Preview(
    name = "Default",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true,
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
    var selectedTab by remember { mutableStateOf(SettingsTab.APPEARANCE) }
    var pendingInterfaceMode by remember { mutableStateOf<AppInterfaceMode?>(null) }
    var openedPicker by remember { mutableStateOf<SettingsTvPicker?>(null) }
    val pickerRowFocusRequesters = remember {
        SettingsTvPicker.entries.associateWith { FocusRequester() }
    }
    val pickerPanelFocusRequester = remember { FocusRequester() }
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

    LaunchedEffect(Unit) {
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

    // Закрытие колонки вариантов возвращает фокус на пункт, который её открыл.
    val closePicker: () -> Unit = {
        openedPicker?.let { runCatching { pickerRowFocusRequesters.getValue(it).requestFocus() } }
        openedPicker = null
    }
    BackHandler(enabled = openedPicker != null, onBack = closePicker)
    LaunchedEffect(openedPicker) {
        if (openedPicker != null) requestFocusUntilTimeout(pickerPanelFocusRequester)
    }
    val pickerRow: SettingsTvPickerRow = { picker, rowModifier ->
        SettingsTvValueRow(
            label = stringResource(picker.titleRes),
            value = picker.valueText(state),
            valueColor = if (picker == SettingsTvPicker.SUBTITLE_COLOR) state.subtitleStyle.textColor.color else null,
            opened = picker == openedPicker,
            onOpen = { openedPicker = picker },
            modifier = rowModifier.focusRequester(pickerRowFocusRequesters.getValue(picker)),
        )
    }

    DisposableEffect(selectedTabFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(selectedTabFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = TvScreenPadding.Horizontal, vertical = TvScreenPadding.Vertical),
    ) {
        // На экране всегда две колонки: с открытыми вариантами список категорий уезжает влево.
        AnimatedVisibility(
            visible = openedPicker == null,
            enter = fadeIn(tween(180)) + expandHorizontally(tween(180)),
            exit = fadeOut(tween(120)) + shrinkHorizontally(tween(120)),
        ) {
            Row {
                SettingsTvCategoryList(
                    selectedTab = selectedTab,
                    tabFocusRequesters = tabFocusRequesters,
                    contentFocusRequesters = contentFocusRequesters,
                    mainMenuFocusRequester = mainMenuFocusRequester,
                    onSelectedTabChanged = {
                        selectedTab = it
                        openedPicker = null
                    },
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                )
                Spacer(modifier = Modifier.width(32.dp))
            }
        }

        SettingsTvPanelHost(
            state = state,
            selectedTab = selectedTab,
            tabFocusRequesters = tabFocusRequesters,
            contentFocusRequesters = contentFocusRequesters,
            pickerRow = pickerRow,
            onEvent = onEvent,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        )

        AnimatedVisibility(
            visible = openedPicker != null,
            enter = fadeIn(tween(180)) + expandHorizontally(tween(180)),
            exit = fadeOut(tween(120)) + shrinkHorizontally(tween(120)),
        ) {
            // Держим последний пикер, пока колонка уезжает.
            var shownPicker by remember { mutableStateOf(openedPicker) }
            openedPicker?.let { shownPicker = it }
            shownPicker?.let { picker ->
                Row {
                    Spacer(modifier = Modifier.width(24.dp))
                    SettingsTvPickerPanel(
                        picker = picker,
                        state = state,
                        entryFocusRequester = pickerPanelFocusRequester,
                        openerFocusRequester = pickerRowFocusRequesters.getValue(picker),
                        onInterfaceModeSelected = { selectedMode ->
                            if (selectedMode != state.interfaceMode) {
                                pendingInterfaceMode = selectedMode
                            }
                        },
                        onEvent = onEvent,
                        onClose = closePicker,
                        modifier = Modifier
                            .width(420.dp)
                            .fillMaxHeight(),
                    )
                }
            }
        }
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
