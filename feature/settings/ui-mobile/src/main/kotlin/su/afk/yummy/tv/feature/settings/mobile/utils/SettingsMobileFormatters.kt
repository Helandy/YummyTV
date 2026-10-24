package su.afk.yummy.tv.feature.settings.mobile.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.designsystem.theme.isDynamicColorSupported
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.BrowserUserAgentProfile
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.feature.settings.presentation.R
import su.afk.yummy.tv.feature.settings.common.utils.hint
import su.afk.yummy.tv.feature.settings.common.utils.label

@Composable
internal fun LibraryContinueWatchingCardSize.hint(): String = stringResource(
    when (this) {
        LibraryContinueWatchingCardSize.COMPACT ->
            R.string.settings_library_continue_watching_card_size_compact_hint

        LibraryContinueWatchingCardSize.STANDARD ->
            R.string.settings_library_continue_watching_card_size_standard_hint

        LibraryContinueWatchingCardSize.LARGE ->
            R.string.settings_library_continue_watching_card_size_large_hint
    },
)

@Composable
internal fun PlayerOrientationMode.label(): String = stringResource(
    when (this) {
        PlayerOrientationMode.SYSTEM -> R.string.settings_player_orientation_system
        PlayerOrientationMode.LEFT -> R.string.settings_player_orientation_left
        PlayerOrientationMode.RIGHT -> R.string.settings_player_orientation_right
    },
)

@Composable
internal fun PlayerOrientationMode.hint(): String = stringResource(
    when (this) {
        PlayerOrientationMode.SYSTEM -> R.string.settings_player_orientation_system_hint
        PlayerOrientationMode.LEFT -> R.string.settings_player_orientation_left_hint
        PlayerOrientationMode.RIGHT -> R.string.settings_player_orientation_right_hint
    },
)

internal fun PlayerOrientationMode.icon(): ImageVector = when (this) {
    PlayerOrientationMode.SYSTEM -> Icons.Rounded.ScreenRotation
    PlayerOrientationMode.RIGHT -> Icons.Rounded.RotateRight
    PlayerOrientationMode.LEFT -> Icons.Rounded.RotateLeft
}
