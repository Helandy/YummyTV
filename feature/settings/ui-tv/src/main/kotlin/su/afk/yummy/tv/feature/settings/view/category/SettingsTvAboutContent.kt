package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.feature.settings.BuildConfig
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.view.AboutRow
import su.afk.yummy.tv.feature.settings.view.ReleaseNotesTvDialog
import su.afk.yummy.tv.feature.settings.view.SettingsDivider

@Composable
internal fun SettingsTvAboutContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
) {
    val repositoryUrl = stringResource(R.string.settings_repository_url)
    val context = LocalContext.current
    var showReleaseNotes by rememberSaveable { mutableStateOf(false) }

    AboutRow(
        label = stringResource(R.string.settings_tv_release_notes_label),
        hint = stringResource(R.string.settings_tv_release_notes_hint),
        modifier = Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
        onClick = { showReleaseNotes = true },
    )
    SettingsDivider()
    AboutRow(
        label = stringResource(R.string.settings_version_label),
        hint = BuildConfig.VERSION_NAME,
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    if (state.isFallbackSessionStorage) {
        SettingsDivider()
        AboutRow(
            label = stringResource(R.string.settings_session_storage_label),
            hint = stringResource(R.string.settings_session_storage_fallback),
            modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
        )
    }
    SettingsDivider()
    AboutRow(
        label = stringResource(R.string.settings_feedback_label),
        hint = repositoryUrl,
        modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
        onClick = { context.openExternalUri(repositoryUrl) },
    )

    if (showReleaseNotes) {
        LaunchedEffect(Unit) { onEvent(SettingsState.Event.ReleaseNotesRequested) }
        ReleaseNotesTvDialog(
            status = state.releaseNotes,
            onRetry = { onEvent(SettingsState.Event.ReleaseNotesRequested) },
            onDismiss = { showReleaseNotes = false },
        )
    }
}
