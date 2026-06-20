package su.afk.yummy.tv.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.PreviewCacheSize
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import su.afk.yummy.tv.core.utils.openExternalUri
import su.afk.yummy.tv.feature.settings.mobile.BuildConfig
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePickerOption
import su.afk.yummy.tv.feature.settings.mobile.utils.hint
import su.afk.yummy.tv.feature.settings.mobile.utils.label
import su.afk.yummy.tv.feature.settings.view.SettingsMobileAboutRow
import su.afk.yummy.tv.feature.settings.view.SettingsMobileNavigationRow
import su.afk.yummy.tv.feature.settings.view.SettingsMobileOptionRow
import su.afk.yummy.tv.feature.settings.view.SettingsMobilePickerSheet
import su.afk.yummy.tv.feature.settings.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.view.SettingsMobileToggleRow

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsMobileScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    var activePicker by remember { mutableStateOf<SettingsMobilePicker?>(null) }
    val title = stringResource(R.string.settings_mobile_title)
    val context = LocalContext.current
    val repositoryUrl = stringResource(R.string.settings_repository_url)

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
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_appearance)) {
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_mobile_theme),
                        value = state.appTheme.label(),
                        hint = state.appTheme.hint(),
                        onClick = { activePicker = SettingsMobilePicker.THEME },
                    )
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_mobile_poster_size),
                        value = state.posterCardSize.label(),
                        hint = state.posterCardSize.hint(),
                        onClick = { activePicker = SettingsMobilePicker.POSTER_SIZE },
                    )
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_library_continue_watching_card_size_title),
                        value = state.libraryContinueWatchingCardSize.label(),
                        hint = state.libraryContinueWatchingCardSize.hint(),
                        onClick = {
                            activePicker = SettingsMobilePicker.LIBRARY_CONTINUE_WATCHING_SIZE
                        },
                    )
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_mobile_poster_quality),
                        value = state.posterQuality.label(),
                        hint = state.posterQuality.hint(),
                        onClick = { activePicker = SettingsMobilePicker.POSTER_QUALITY },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_player)) {
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_mobile_default_player),
                        value = state.preferredPlayer.label(),
                        hint = state.preferredPlayer.hint(),
                        onClick = { activePicker = SettingsMobilePicker.PLAYER },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_auto_skip_label),
                        hint = if (state.autoSkipOpeningsEndings) {
                            stringResource(R.string.settings_auto_skip_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.autoSkipOpeningsEndings,
                        onClick = { onEvent(SettingsState.Event.AutoSkipOpeningsEndingsToggled) },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_suggest_next_episode_on_watched_label),
                        hint = if (state.suggestNextEpisodeOnWatched) {
                            stringResource(R.string.settings_suggest_next_episode_on_watched_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.suggestNextEpisodeOnWatched,
                        onClick = {
                            onEvent(SettingsState.Event.SuggestNextEpisodeOnWatchedToggled)
                        },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_refresh_continue_watching_progress_label),
                        hint = if (state.refreshContinueWatchingProgressOnLaunch) {
                            stringResource(R.string.settings_refresh_continue_watching_progress_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.refreshContinueWatchingProgressOnLaunch,
                        onClick = {
                            onEvent(
                                SettingsState.Event.RefreshContinueWatchingProgressOnLaunchToggled,
                            )
                        },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_details)) {
                    SettingsMobileNavigationRow(
                        label = stringResource(R.string.settings_details_buttons_order),
                        hint = stringResource(R.string.settings_details_buttons_order_hint),
                        onClick = { onEvent(SettingsState.Event.DetailsButtonOrderSelected) },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_cache)) {
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_mobile_preview_cache),
                        value = state.previewCacheSize.label(),
                        hint = state.previewCacheSize.hint(),
                        onClick = { activePicker = SettingsMobilePicker.CACHE },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_language)) {
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_content_language_title),
                        value = state.contentLanguage.label(),
                        onClick = { activePicker = SettingsMobilePicker.CONTENT_LANGUAGE },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_api)) {
                    OutlinedTextField(
                        value = state.yaniApplicationToken,
                        onValueChange = { onEvent(SettingsState.Event.YaniApplicationTokenChanged(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        label = { Text(stringResource(R.string.settings_yani_application_token_label)) },
                        placeholder = { Text(stringResource(R.string.settings_yani_application_token_placeholder)) },
                        supportingText = { Text(stringResource(R.string.settings_yani_application_token_hint)) },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_about)) {
                    SettingsMobileAboutRow(
                        label = stringResource(R.string.settings_version_label),
                        hint = BuildConfig.VERSION_NAME,
                    )
                    SettingsMobileAboutRow(
                        label = stringResource(R.string.settings_feedback_label),
                        hint = repositoryUrl,
                        onClick = { context.openExternalUri(repositoryUrl) },
                    )
                }
            }
        }
    }

    when (activePicker) {
        SettingsMobilePicker.THEME -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_theme),
            selectedValue = state.appTheme,
            options = AppTheme.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.AppThemeSelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.POSTER_QUALITY -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_poster_quality),
            selectedValue = state.posterQuality,
            options = PosterQuality.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PosterQualitySelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.POSTER_SIZE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_poster_size),
            selectedValue = state.posterCardSize,
            options = PosterCardSize.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PosterCardSizeSelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.LIBRARY_CONTINUE_WATCHING_SIZE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_library_continue_watching_card_size_title),
            selectedValue = state.libraryContinueWatchingCardSize,
            options = LibraryContinueWatchingCardSize.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.LibraryContinueWatchingCardSizeSelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.PLAYER -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_default_player),
            selectedValue = state.preferredPlayer,
            options = PreferredPlayer.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PreferredPlayerSelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.CACHE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_preview_cache),
            selectedValue = state.previewCacheSize,
            options = PreviewCacheSize.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PreviewCacheSizeSelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.CONTENT_LANGUAGE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_content_language_title),
            selectedValue = state.contentLanguage,
            options = YaniContentLanguage.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.ContentLanguageSelected(it))
                activePicker = null
            },
        )

        null -> Unit
    }
}
