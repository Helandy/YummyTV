package su.afk.yummy.tv.feature.player.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

internal class TvPlayerFocusRequesters {
    val play = FocusRequester()
    val quality = FocusRequester()
    val dubbing = FocusRequester()
    val balancer = FocusRequester()
    val speed = FocusRequester()
    val resize = FocusRequester()
    val overlay = FocusRequester()
    val selectedQuality = FocusRequester()
    val selectedDubbing = FocusRequester()
    val selectedBalancer = FocusRequester()
    val selectedSpeed = FocusRequester()
    val selectedResize = FocusRequester()
    val skip = FocusRequester()
    val nextEpisode = FocusRequester()
    val rateTitle = FocusRequester()

    fun control(target: PlayerControlFocusTarget): FocusRequester =
        when (target) {
            PlayerControlFocusTarget.Quality -> quality
            PlayerControlFocusTarget.Dubbing -> dubbing
            PlayerControlFocusTarget.Balancer -> balancer
            PlayerControlFocusTarget.Resize -> resize
            PlayerControlFocusTarget.Speed -> speed
        }

    fun requestControl(target: PlayerControlFocusTarget): Boolean =
        runCatching { control(target).requestFocus() }.isSuccess
}

@Composable
internal fun rememberTvPlayerFocusRequesters(): TvPlayerFocusRequesters =
    remember { TvPlayerFocusRequesters() }
