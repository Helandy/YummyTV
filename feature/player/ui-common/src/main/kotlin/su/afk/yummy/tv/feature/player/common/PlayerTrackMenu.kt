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
    private val offerInStreamTracks: Boolean,
) {
    /** Источник — Alloha: списки и выбор идут через неё. */
    val usesAlloha: Boolean get() = alloha.isAvailable

    // Дорожки внутри потока предлагаем только там, где это настоящий файл с несколькими дорожками
    // (торрент): у Kodik и прочих HLS лишние дорожки бывают служебными.
    val showAudioChoice: Boolean
        get() = if (usesAlloha) alloha.hasAudioChoice else offerInStreamTracks && inStream.hasSelectableAudio
    val showSubtitleChoice: Boolean
        get() = if (usesAlloha) alloha.hasSubtitleChoice else offerInStreamTracks && inStream.hasSelectableText

    val audioOptions: List<PlayerTrackOption>
        get() = if (usesAlloha) alloha.audioOptions else inStream.audioOptions
    val subtitleOptions: List<PlayerTrackOption>
        get() = if (usesAlloha) alloha.subtitleOptions else inStream.textOptions
    val selectedAudioIndex: Int
        get() = if (usesAlloha) alloha.selectedAudioIndex else inStream.selectedAudioIndex
    val selectedSubtitleIndex: Int
        get() = if (usesAlloha) alloha.selectedSubtitleOptionIndex else inStream.selectedTextIndex

    /**
     * @param positionMs позиция, с которой продолжить, если выбор пересоберёт поток (Alloha):
     *   её запоминает ViewModel.
     */
    fun selectAudio(
        index: Int,
        positionMs: Long,
        onEvent: (PlayerState.Event) -> Unit,
    ) {
        if (!usesAlloha) {
            inStream.selectAudio(index)
            return
        }
        val id = alloha.audioIdAt(index) ?: return
        onEvent(PlayerState.Event.AllohaAudioTrackSelected(id, positionMs))
    }

    /** @param positionMs см. [selectAudio]. */
    fun selectSubtitle(
        index: Int,
        positionMs: Long,
        onEvent: (PlayerState.Event) -> Unit,
    ) {
        if (!usesAlloha) {
            inStream.selectText(index)
            return
        }
        onEvent(PlayerState.Event.AllohaSubtitleSelected(alloha.subtitleIndexAt(index), positionMs))
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
    // Метку раздачи ставит только торрент-источник: это файл с набором дорожек, а не HLS-манифест.
    val offerInStreamTracks = !state.streamSourceLabel.isNullOrBlank()
    return remember(alloha, inStream, offerInStreamTracks) {
        PlayerTrackMenu(alloha, inStream, offerInStreamTracks)
    }
}
