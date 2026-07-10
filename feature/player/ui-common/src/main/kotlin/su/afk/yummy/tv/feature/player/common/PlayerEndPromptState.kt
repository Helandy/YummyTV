package su.afk.yummy.tv.feature.player.common

sealed interface PlayerEndPromptState {
    data object Hidden : PlayerEndPromptState
    data object WithoutCountdown : PlayerEndPromptState
    data class WithCountdown(val seconds: Int) : PlayerEndPromptState
}

val PlayerEndPromptState.isVisible: Boolean
    get() = this !is PlayerEndPromptState.Hidden

const val PLAYER_END_PROMPT_COUNTDOWN_SECONDS = 10
