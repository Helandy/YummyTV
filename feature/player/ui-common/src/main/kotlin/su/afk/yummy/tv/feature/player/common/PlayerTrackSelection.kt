package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import su.afk.yummy.tv.feature.player.common.utils.disambiguated
import su.afk.yummy.tv.feature.player.common.utils.readableTrackLabel

/** Одна выбираемая дорожка (озвучка или субтитр) в панели выбора. Индекс — позиция в списке опций. */
data class PlayerTrackOption(val index: Int, val label: String)

/** Синтетический индекс опции "Выключено" для субтитров — всегда первый элемент [PlayerTrackSelectionState.textOptions]. */
const val PLAYER_TEXT_TRACK_OFF_INDEX = 0

/**
 * Native Media3 audio/text track selection поверх текущего [Player] (в отличие от выбора
 * качества, который подменяет весь MediaItem другим URL). Показывает несколько опций только
 * когда сам HLS-манифест реально содержит несколько audio/text groups (у Alloha) — для
 * источников с одной дорожкой [hasSelectableAudio]/[hasSelectableText] остаются false.
 */
@Stable
class PlayerTrackSelectionState internal constructor(
    private val player: Player?,
    private val offLabel: String,
    private val fallbackLabel: (index: Int) -> String,
) {
    var tracks: Tracks by mutableStateOf(player?.currentTracks ?: Tracks.EMPTY)
        internal set

    val audioOptions: List<PlayerTrackOption>
        get() = tracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
            .mapIndexed { index, group -> group.label(index) }
            .disambiguated()
            .mapIndexed(::PlayerTrackOption)

    val textOptions: List<PlayerTrackOption>
        get() = buildList {
            add(PlayerTrackOption(PLAYER_TEXT_TRACK_OFF_INDEX, offLabel))
            tracks.groups
                .filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
                .mapIndexed { index, group -> group.label(index) }
                .disambiguated()
                .forEachIndexed { index, label -> add(PlayerTrackOption(index + 1, label)) }
        }

    val hasSelectableAudio: Boolean get() = audioOptions.size > 1
    val hasSelectableText: Boolean get() = textOptions.size > 1

    val selectedAudioIndex: Int
        get() = tracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
            .indexOfFirst(Tracks.Group::isSelected)
            .coerceAtLeast(0)

    val selectedTextIndex: Int
        get() {
            val selected = tracks.groups
                .filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
                .indexOfFirst(Tracks.Group::isSelected)
            return if (selected < 0) PLAYER_TEXT_TRACK_OFF_INDEX else selected + 1
        }

    fun selectAudio(index: Int) {
        val group = tracks.groups
            .filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }
            .getOrNull(index) ?: return
        val current = player?.trackSelectionParameters ?: return
        player.trackSelectionParameters = current.buildUpon()
            .setOverrideForType(
                TrackSelectionOverride(
                    group.mediaTrackGroup,
                    group.preferredTrackIndex()
                )
            )
            .build()
    }

    fun selectText(index: Int) {
        val current = player?.trackSelectionParameters ?: return
        if (index == PLAYER_TEXT_TRACK_OFF_INDEX) {
            player.trackSelectionParameters = current.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            return
        }
        val group = tracks.groups
            .filter { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
            .getOrNull(index - 1) ?: return
        player.trackSelectionParameters = current.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setOverrideForType(
                TrackSelectionOverride(
                    group.mediaTrackGroup,
                    group.preferredTrackIndex()
                )
            )
            .build()
    }

    private fun Tracks.Group.preferredTrackIndex(): Int =
        (0 until length).firstOrNull(::isTrackSupported) ?: 0

    private fun Tracks.Group.label(index: Int): String {
        val format = (0 until length).firstOrNull(::isTrackSupported)?.let(::getTrackFormat)
        return format?.readableTrackLabel(fallbackLabel(index)) ?: fallbackLabel(index)
    }
}

@Composable
fun rememberPlayerTrackSelection(
    player: Player?,
    offLabel: String,
    fallbackLabel: (index: Int) -> String,
): PlayerTrackSelectionState {
    val state = remember(player, offLabel, fallbackLabel) {
        PlayerTrackSelectionState(player, offLabel, fallbackLabel)
    }
    DisposableEffect(player) {
        if (player == null) {
            onDispose {}
        } else {
            state.tracks = player.currentTracks
            val listener = object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    state.tracks = tracks
                }
            }
            player.addListener(listener)
            onDispose { player.removeListener(listener) }
        }
    }
    return state
}
