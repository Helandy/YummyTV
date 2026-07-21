package su.afk.yummy.tv.feature.player.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.mobile.R
import su.afk.yummy.tv.feature.player.mobile.model.GestureTutorialStep

@Composable
internal fun GestureTutorialStep.text(): Pair<String, String> = when (this) {
    GestureTutorialStep.Tap -> stringResource(
        R.string.player_mobile_gesture_tutorial_tap_title
    ) to stringResource(R.string.player_mobile_gesture_tutorial_tap_description)

    GestureTutorialStep.Seek -> stringResource(
        R.string.player_mobile_gesture_tutorial_seek_title
    ) to stringResource(R.string.player_mobile_gesture_tutorial_seek_description)

    GestureTutorialStep.Vertical -> stringResource(
        R.string.player_mobile_gesture_tutorial_vertical_title
    ) to stringResource(R.string.player_mobile_gesture_tutorial_vertical_description)

    GestureTutorialStep.Zoom -> stringResource(
        R.string.player_mobile_gesture_tutorial_zoom_title
    ) to stringResource(R.string.player_mobile_gesture_tutorial_zoom_description)

    GestureTutorialStep.Hold -> stringResource(
        R.string.player_mobile_gesture_tutorial_hold_title
    ) to stringResource(R.string.player_mobile_gesture_tutorial_hold_description)
}
