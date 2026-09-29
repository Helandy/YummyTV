package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.media3.common.Player
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Меню аудиодорожек и субтитров, общее для ТВ и мобилки. Alloha отдаёт собственные списки
 * (выбор уходит во ViewModel и пересобирает поток), остальные источники — дорожки из самого
 * потока (выбор применяется к плееру напрямую).
 */
@Stable
class PlayerTrackMenu internal constructor(
    private val alloha: PlayerAllohaTracks,
    private val inStream: PlayerTrackSelectionState,
) {
    /** Источник — Alloha: списки и выбор идут через неё. */
    val usesAlloha: Boolean get() = alloha.isAvailable

    // Панель/вкладка подписана «Alloha», поэтому показывается только для самой Alloha, а не для
    // любого потока с лишними дорожками (Kodik такие иногда отдаёт).
    val showAudioChoice: Boolean get() = usesAlloha && alloha.hasAudioChoice
    val showSubtitleChoice: Boolean get() = usesAlloha && alloha.hasSubtitleChoice

    val audioOptions: List<PlayerTrackOption>
        get() = if (usesAlloha) alloha.audioOptions else inStream.audioOptions
    val subtitleOptions: List<PlayerTrackOption>
        get() = if (usesAlloha) alloha.subtitleOptions else inStream.textOptions
    val selectedAudioIndex: Int
        get() = if (usesAlloha) alloha.selectedAudioIndex else inStream.selectedAudioIndex
    val selectedSubtitleIndex: Int
        get() = if (usesAlloha) alloha.selectedSubtitleOptionIndex else inStream.selectedTextIndex

    /**
     * @return true, если выбор пересоберёт поток (Alloha) — платформе стоит запомнить позицию,
     *   с которой продолжить.
     */
    fun selectAudio(
        index: Int,
        positionMs: Long,
        onEvent: (PlayerState.Event) -> Unit,
    ): Boolean {
        if (!usesAlloha) {
            inStream.selectAudio(index)
            return false
        }
        val id = alloha.audioIdAt(index) ?: return false
        onEvent(PlayerState.Event.AllohaAudioTrackSelected(id, positionMs))
        return true
    }

    /** @return true, если выбор пересоберёт поток (Alloha). */
    fun selectSubtitle(
        index: Int,
        onEvent: (PlayerState.Event) -> Unit,
    ): Boolean {
        if (!usesAlloha) {
            inStream.selectText(index)
            return false
        }
        onEvent(PlayerState.Event.AllohaSubtitleSelected(alloha.subtitleIndexAt(index)))
        return true
    }
}

@Composable
fun rememberPlayerTrackMenu(
    player: Player,
    state: PlayerState.State,
): PlayerTrackMenu {
    val subtitlesOffLabel = stringResource(R.string.player_subtitles_off)
    val trackFallbackTemplate = stringResource(R.string.player_track_fallback)
    val inStream = rememberPlayerTrackSelection(
        player = player,
        offLabel = subtitlesOffLabel,
        fallbackLabel = { index -> trackFallbackTemplate.format(index + 1) },
    )
    val alloha = remember(
        state.allohaAudioTracks,
        state.selectedAllohaAudioId,
        state.allohaSubtitles,
        state.selectedAllohaSubtitleIndex,
        subtitlesOffLabel,
    ) {
        PlayerAllohaTracks(
            audioTracks = state.allohaAudioTracks,
            selectedAudioId = state.selectedAllohaAudioId,
            subtitles = state.allohaSubtitles,
            selectedSubtitleIndex = state.selectedAllohaSubtitleIndex,
            subtitlesOffLabel = subtitlesOffLabel,
        )
    }
    return remember(alloha, inStream) { PlayerTrackMenu(alloha, inStream) }
}
