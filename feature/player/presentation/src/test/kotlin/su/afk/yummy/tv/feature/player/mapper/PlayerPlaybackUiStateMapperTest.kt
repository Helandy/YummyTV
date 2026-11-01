package su.afk.yummy.tv.feature.player.mapper

import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.PlayerState

class PlayerPlaybackUiStateMapperTest : BaseUnitTest() {

    @Test
    fun `ошибка из-за сети не называет виноватым плеер`() {
        val state = PlayerState.State(playerError = NO_CONNECTION)

        val ui = state.toPlayerPlaybackUiState(playerNamePrefix = "Плеер ", connectionErrorMessage = NO_CONNECTION)

        assertNull(ui.errorBalancerLabel)
        org.junit.Assert.assertFalse(ui.canChangePlayer)
        org.junit.Assert.assertFalse(ui.canChangeDubbing)
    }

    private companion object {
        const val NO_CONNECTION = "Нет подключения к интернету"
    }
}
