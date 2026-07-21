package su.afk.yummy.tv.feature.player.common.model

sealed interface PlayerEndPromptState {
    data object Hidden : PlayerEndPromptState
    data object WithoutCountdown : PlayerEndPromptState
    data class WithCountdown(val seconds: Int) : PlayerEndPromptState
}
