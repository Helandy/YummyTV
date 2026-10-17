package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.presentation.R

/** Выбор дорожки субтитров («Выкл» первым пунктом) в боковой панели плеера. */
@Composable
internal fun MobilePlayerSubtitleTrackSection(
    trackNames: List<String>,
    selectedIndex: Int,
    onTrackSelected: (Int) -> Unit,
) {
    MobilePlayerSettingsSection(
        title = stringResource(R.string.player_subtitle_track_title),
        icon = Icons.Filled.Subtitles,
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
