package su.afk.yummy.tv.feature.player.common

sealed interface PlayerEndPromptState {
    data object Hidden : PlayerEndPromptState
    data object WithoutCountdown : PlayerEndPromptState
    data class WithCountdown(val seconds: Int) : PlayerEndPromptState
}

val PlayerEndPromptState.isVisible: Boolean
    get() = this !is PlayerEndPromptState.Hidden

fun playerEndPromptFor(autoPlayNextEpisode: Boolean): PlayerEndPromptState =
    if (autoPlayNextEpisode) {
        PlayerEndPromptState.WithCountdown(PLAYER_END_PROMPT_COUNTDOWN_SECONDS)
    } else {
        PlayerEndPromptState.WithoutCountdown
    }

/** ON_PAUSE: активный отсчёт вырождается в промпт без отсчёта. */
fun PlayerEndPromptState.downgradedCountdown(): PlayerEndPromptState =
    if (this is PlayerEndPromptState.WithCountdown) {
        PlayerEndPromptState.WithoutCountdown
    } else {
        this
    }

const val PLAYER_END_PROMPT_COUNTDOWN_SECONDS = 10
