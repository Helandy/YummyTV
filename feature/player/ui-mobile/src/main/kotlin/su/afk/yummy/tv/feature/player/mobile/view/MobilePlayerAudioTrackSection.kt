package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.presentation.R

/** Выбор озвучки Alloha в боковой панели плеера, рядом с дорожкой субтитров. */
@Composable
internal fun MobilePlayerAudioTrackSection(
    trackNames: List<String>,
    selectedIndex: Int,
    onTrackSelected: (Int) -> Unit,
) {
    MobilePlayerSettingsSection(
        title = stringResource(R.string.player_audio_track_title),
        icon = Icons.Filled.Audiotrack,
    ) {
        trackNames.forEachIndexed { index, name ->
            MobilePlayerSelectionRow(
                label = name,
                selected = index == selectedIndex,
                onClick = { onTrackSelected(index) },
            )
        }
    }
}
