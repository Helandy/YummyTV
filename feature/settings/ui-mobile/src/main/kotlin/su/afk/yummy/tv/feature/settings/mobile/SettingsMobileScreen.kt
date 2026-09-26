package su.afk.yummy.tv.feature.settings.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.model.hintRes
import su.afk.yummy.tv.feature.settings.mobile.model.icon
import su.afk.yummy.tv.feature.settings.mobile.model.titleRes
import su.afk.yummy.tv.feature.settings.mobile.view.ReleaseNotesMobileSheet
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileAboutRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileEffects
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileNavigationRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.navigator.SettingsCategory

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        SettingsMobileScreen(SettingsState.State(), emptyFlow()) {}
    }

/** Корневой экран мобильных настроек: список категорий, у каждой свой экран. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsMobileScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    val context = LocalContext.current
    val repositoryUrl = stringResource(R.string.settings_repository_url)
    val firstCategoryFocusRequester = remember { FocusRequester() }
    var showReleaseNotes by rememberSaveable { mutableStateOf(false) }

    SettingsMobileEffects(effect = effect, onEvent = onEvent)

    if (showReleaseNotes) {
        LaunchedEffect(Unit) { onEvent(SettingsState.Event.ReleaseNotesRequested) }
        ReleaseNotesMobileSheet(
            status = state.releaseNotes,
            onRetry = { onEvent(SettingsState.Event.ReleaseNotesRequested) },
            onDismiss = { showReleaseNotes = false },
        )
    }

    LaunchedEffect(Unit) {
        requestFocusUntilTimeout(firstCategoryFocusRequester)
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.settings_mobile_title),
                onBack = { onEvent(SettingsState.Event.BackSelected) },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.mobileContentMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "categories") {
                SettingsMobileSection {
                    SettingsCategory.entries.forEachIndexed { index, category ->
                        SettingsMobileNavigationRow(
                            label = stringResource(category.titleRes),
                            hint = stringResource(category.hintRes),
                            icon = category.icon,
                            onClick = { onEvent(SettingsState.Event.CategorySelected(category)) },
                            modifier = if (index == 0) {
                                Modifier.focusRequester(firstCategoryFocusRequester)
                            } else {
                                Modifier
                            },
                        )
                    }
                }
            }

            item(key = "about") {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_about)) {
                    SettingsMobileAboutRow(
                        label = stringResource(R.string.settings_mobile_release_notes_label),
                        hint = stringResource(R.string.settings_mobile_release_notes_hint),
                        onClick = { showReleaseNotes = true },
                    )
                    SettingsMobileAboutRow(
                        label = stringResource(R.string.settings_version_label),
                        hint = BuildConfig.VERSION_NAME,
                    )
                    if (state.isFallbackSessionStorage) {
                        SettingsMobileAboutRow(
                            label = stringResource(R.string.settings_session_storage_label),
                            hint = stringResource(R.string.settings_session_storage_fallback),
                        )
                    }
                    SettingsMobileAboutRow(
                        label = stringResource(R.string.settings_feedback_label),
                        hint = repositoryUrl,
                        onClick = { context.openExternalUri(repositoryUrl) },
                    )
                }
            }
        }
    }
}
