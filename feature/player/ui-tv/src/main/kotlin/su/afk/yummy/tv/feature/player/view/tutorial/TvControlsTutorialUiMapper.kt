package su.afk.yummy.tv.feature.player.view.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.model.TvControlsTutorialStep
import su.afk.yummy.tv.feature.player.presentation.R

internal fun TvControlsTutorialStep.titleRes(): Int = when (this) {
    TvControlsTutorialStep.ShowControls -> R.string.player_tv_controls_tutorial_show_controls_title
    TvControlsTutorialStep.Seek -> R.string.player_tv_controls_tutorial_seek_title
    TvControlsTutorialStep.Navigate -> R.string.player_tv_controls_tutorial_navigate_title
    TvControlsTutorialStep.PlayPause -> R.string.player_tv_controls_tutorial_play_pause_title
    TvControlsTutorialStep.Panels -> R.string.player_tv_controls_tutorial_panels_title
    TvControlsTutorialStep.Exit -> R.string.player_tv_controls_tutorial_exit_title
}

internal fun TvControlsTutorialStep.descriptionRes(): Int = when (this) {
    TvControlsTutorialStep.ShowControls ->
        R.string.player_tv_controls_tutorial_show_controls_description

    TvControlsTutorialStep.Seek -> R.string.player_tv_controls_tutorial_seek_description
    TvControlsTutorialStep.Navigate -> R.string.player_tv_controls_tutorial_navigate_description
    TvControlsTutorialStep.PlayPause ->
        R.string.player_tv_controls_tutorial_play_pause_description

    TvControlsTutorialStep.Panels -> R.string.player_tv_controls_tutorial_panels_description
    TvControlsTutorialStep.Exit -> R.string.player_tv_controls_tutorial_exit_description
}

@Composable
internal fun TvControlsTutorialStep.text(): Pair<String, String> =
    stringResource(titleRes()) to stringResource(descriptionRes())
