package su.afk.yummy.tv.feature.settings.mobile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.model.titleRes
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileDialogsHost
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileEffects
import su.afk.yummy.tv.feature.settings.mobile.view.category.SettingsMobileApiContent
import su.afk.yummy.tv.feature.settings.mobile.view.category.SettingsMobileAppearanceContent
import su.afk.yummy.tv.feature.settings.mobile.view.category.SettingsMobileGeneralContent
import su.afk.yummy.tv.feature.settings.mobile.view.category.SettingsMobilePlaybackContent
import su.afk.yummy.tv.feature.settings.mobile.view.category.SettingsMobilePlayerContent
import su.afk.yummy.tv.feature.settings.mobile.view.category.SettingsMobileStorageContent
import su.afk.yummy.tv.feature.settings.mobile.view.category.SettingsMobileSubtitlesContent
import su.afk.yummy.tv.feature.settings.mobile.view.category.SettingsMobileWatchProgressContent
import su.afk.yummy.tv.feature.settings.mobile.view.rememberSettingsMobileDialogs
import su.afk.yummy.tv.feature.settings.navigator.SettingsCategory

private class SettingsCategoryPreviewProvider : PreviewParameterProvider<SettingsCategory> {
    override val values = SettingsCategory.entries.asSequence()
}

@Preview(name = "Category", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun SettingsMobileCategoryScreenPreview(
    @PreviewParameter(SettingsCategoryPreviewProvider::class) category: SettingsCategory,
) = ScreenPreviewTheme {
    SettingsMobileCategoryScreen(category, SettingsState.State(), emptyFlow()) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsMobileCategoryScreen(
    category: SettingsCategory,
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    val dialogs = rememberSettingsMobileDialogs()
    val onPickerRequested = { picker: SettingsMobilePicker ->
        dialogs.activePicker = picker
    }

    SettingsMobileEffects(effect = effect, onEvent = onEvent)

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(category.titleRes),
                onBack = { onEvent(SettingsState.Event.BackSelected) },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier
                .mobileContentMaxWidth()
                .imePadding(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
        ) {
            item(key = category.name) {
                when (category) {
                    SettingsCategory.GENERAL ->
                        SettingsMobileGeneralContent(state, onEvent, onPickerRequested)

                    SettingsCategory.APPEARANCE ->
                        SettingsMobileAppearanceContent(state, onEvent, onPickerRequested)

                    SettingsCategory.PLAYER ->
                        SettingsMobilePlayerContent(state, onEvent, onPickerRequested)

                    SettingsCategory.PLAYBACK -> SettingsMobilePlaybackContent(state, onEvent)

                    SettingsCategory.WATCH_PROGRESS -> SettingsMobileWatchProgressContent(state, onEvent)

                    SettingsCategory.SUBTITLES ->
                        SettingsMobileSubtitlesContent(state, onEvent, onPickerRequested)

                    SettingsCategory.STORAGE -> SettingsMobileStorageContent(
                        state = state,
                        onEvent = onEvent,
                        onCacheStorageRequested = { dialogs.showCacheStorage = true },
                    )

                    SettingsCategory.API -> SettingsMobileApiContent(state, onEvent)
                }
            }
        }
    }

    SettingsMobileDialogsHost(dialogs = dialogs, state = state, onEvent = onEvent)
}
