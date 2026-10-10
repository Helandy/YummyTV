package su.afk.yummy.tv.feature.player.common.utils

import android.content.Context
import androidx.annotation.StringRes
import androidx.media3.common.Player
import su.afk.yummy.tv.core.utils.formatting.millisToClockTime
import su.afk.yummy.tv.feature.player.PlayerSkips
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.common.PlayerSeekController
import su.afk.yummy.tv.feature.player.common.PlayerSkipUiState
import su.afk.yummy.tv.feature.player.common.model.PlayerActiveSkip
import su.afk.yummy.tv.feature.player.model.PlayerSkipType
import su.afk.yummy.tv.feature.player.presentation.R

fun currentSkip(
    skips: PlayerSkips,
    positionMs: Long,
    dismissedKeys: List<String>,
): PlayerActiveSkip? =
    listOfNotNull(
        skips.opening?.let {
            PlayerActiveSkip(
                "opening:${it.startMs}:${it.endMs}",
                PlayerSkipType.Opening,
                it
            )
        },
        skips.ending?.let {
            PlayerActiveSkip(
                "ending:${it.startMs}:${it.endMs}",
                PlayerSkipType.Ending,
                it
            )
        },
    ).firstOrNull { skip ->
        skip.key !in dismissedKeys && positionMs in skip.segment.startMs..skip.segment.endMs
    }

@StringRes
fun PlayerSkipType.skippedMessageRes(): Int =
    when (this) {
        PlayerSkipType.Opening -> R.string.player_opening_skipped
        PlayerSkipType.Ending -> R.string.player_ending_skipped
    }

/**
 * Пропуск опенинга/эндинга, общий для ТВ и мобилки: сегмент больше не предлагается в этой серии,
 * снекбар «пропущено», отчёт о ручном выборе и перемотка к концу сегмента.
 *
 * @param reportSelection false для автопропуска: он не считается выбором пользователя.
 */
fun skipPlayerSegment(
    skip: PlayerActiveSkip,
    context: Context,
    player: Player,
    skipUi: PlayerSkipUiState,
    seekController: PlayerSeekController,
    reportSelection: Boolean,
    onEvent: (PlayerState.Event) -> Unit,
) {
    if (skip.key !in skipUi.dismissedSkipKeys) skipUi.dismissedSkipKeys += skip.key
    skipUi.showSnackbar(
        context.getString(
            skip.type.skippedMessageRes(),
            skip.segment.startMs.millisToClockTime(),
            skip.segment.endMs.millisToClockTime(),
        )
    )
    if (reportSelection) {
        onEvent(
            PlayerState.Event.SkipSegmentSelected(
                type = skip.type,
                fromMs = player.currentPosition.coerceAtLeast(0L),
                toMs = skip.segment.endMs,
            )
        )
    }
    seekController.seekTo(skip.segment.endMs)
}
