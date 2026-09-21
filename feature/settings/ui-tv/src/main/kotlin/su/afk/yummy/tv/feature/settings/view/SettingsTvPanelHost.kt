package su.afk.yummy.tv.feature.settings.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.SettingsTab
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvAboutContent
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvApiContent
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvAppearanceContent
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvGeneralContent
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvPickerRow
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvPlaybackContent
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvPlayerContent
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvStorageContent
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvSubtitlesContent
import su.afk.yummy.tv.feature.settings.view.category.SettingsTvWatchProgressContent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SettingsTvPanelHost(
    state: SettingsState.State,
    selectedTab: SettingsTab,
    tabFocusRequesters: Map<SettingsTab, FocusRequester>,
    contentFocusRequesters: Map<SettingsTab, FocusRequester>,
    pickerRow: SettingsTvPickerRow,
    onEvent: (SettingsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Выход фокуса влево из панели всегда ведёт к текущей категории слева
            // (иначе пространственный поиск с нижних строк уводит в соседнюю категорию).
            .focusProperties {
                onExit = {
                    if (requestedFocusDirection == FocusDirection.Left) {
                        tabFocusRequesters.getValue(selectedTab).requestFocus()
                    }
                }
            }
            .focusGroup(),
        contentAlignment = Alignment.TopStart,
    ) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "settings_tab_content",
            modifier = Modifier.widthIn(max = 720.dp),
        ) { tab ->
            val tabFocusRequester = tabFocusRequesters.getValue(tab)
            val tabContentFocusRequester = contentFocusRequesters.getValue(tab)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (tab) {
                    SettingsTab.GENERAL -> SettingsTvGeneralContent(
                        state = state,
                        tabFocusRequester = tabFocusRequester,
                        tabContentFocusRequester = tabContentFocusRequester,
                        onEvent = onEvent,
                        pickerRow = pickerRow,
                    )

                    SettingsTab.APPEARANCE -> SettingsTvAppearanceContent(
                        state,
                        tabFocusRequester,
                        tabContentFocusRequester,
                        onEvent,
                        pickerRow,
                    )

                    SettingsTab.PLAYER -> SettingsTvPlayerContent(
                        state,
                        tabFocusRequester,
                        tabContentFocusRequester,
                        onEvent,
                        pickerRow,
                    )

                    SettingsTab.PLAYBACK -> SettingsTvPlaybackContent(
                        state,
                        tabFocusRequester,
                        tabContentFocusRequester,
                        onEvent,
                    )

                    SettingsTab.WATCH_PROGRESS -> SettingsTvWatchProgressContent(
                        state,
                        tabFocusRequester,
                        tabContentFocusRequester,
                        onEvent,
                    )

                    SettingsTab.SUBTITLES -> SettingsTvSubtitlesContent(
                        state,
                        tabFocusRequester,
                        tabContentFocusRequester,
                        onEvent,
                        pickerRow,
                    )

                    SettingsTab.STORAGE -> SettingsTvStorageContent(
                        state,
                        tabFocusRequester,
                        tabContentFocusRequester,
                        onEvent,
                    )

                    SettingsTab.API -> SettingsTvApiContent(
                        state,
                        tabFocusRequester,
                        tabContentFocusRequester,
                        onEvent,
                    )

                    SettingsTab.ABOUT -> SettingsTvAboutContent(
                        state,
                        tabFocusRequester,
                        tabContentFocusRequester,
                        onEvent,
                    )
                }
            }
        }
    }
}
