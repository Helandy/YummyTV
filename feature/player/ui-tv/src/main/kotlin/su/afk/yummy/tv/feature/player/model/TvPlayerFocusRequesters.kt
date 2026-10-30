package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

internal class TvPlayerFocusRequesters {
    val play = FocusRequester()
    val progress = FocusRequester()
    val quality = FocusRequester()
    val dubbing = FocusRequester()
    val balancer = FocusRequester()
    val episodes = FocusRequester()
    val playerSettings = FocusRequester()
    val alloha = FocusRequester()
    val overlay = FocusRequester()
    val selectedQuality = FocusRequester()
    val selectedDubbing = FocusRequester()
    val selectedBalancer = FocusRequester()
    val selectedEpisodes = FocusRequester()
    val selectedPlayerSettings = FocusRequester()
    val selectedAlloha = FocusRequester()
    val skip = FocusRequester()
    val stay = FocusRequester()
    val nextEpisode = FocusRequester()
    val nextEpisodeControl = FocusRequester()
    val nextEpisodeUnavailable = FocusRequester()
    val finalEpisodeAction = FocusRequester()

    fun control(target: PlayerControlFocusTarget): FocusRequester =
        when (target) {
            PlayerControlFocusTarget.Quality -> quality
            PlayerControlFocusTarget.Dubbing -> dubbing
            PlayerControlFocusTarget.Balancer -> balancer
            PlayerControlFocusTarget.Episodes -> episodes
            PlayerControlFocusTarget.PlayerSettings -> playerSettings
            PlayerControlFocusTarget.Alloha -> alloha
            PlayerControlFocusTarget.NextEpisode -> nextEpisodeControl
        }

    fun requestControl(target: PlayerControlFocusTarget): Boolean =
        runCatching { control(target).requestFocus() }.isSuccess
}

@Composable
internal fun rememberTvPlayerFocusRequesters(): TvPlayerFocusRequesters =
    remember { TvPlayerFocusRequesters() }
