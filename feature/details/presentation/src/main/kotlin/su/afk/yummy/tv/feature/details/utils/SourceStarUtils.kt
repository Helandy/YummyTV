package su.afk.yummy.tv.feature.details.utils

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.common.player.isStarredBalancer
import su.afk.yummy.tv.core.common.player.isStarredDubbing
import su.afk.yummy.tv.feature.details.details.model.BalancerOption
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.model.DubbingOption
import su.afk.yummy.tv.feature.details.details.model.DubbingPickerState
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsState

private fun <T> List<T>.starredFirstThenBase(
    isStarred: (T) -> Boolean,
    baseIndex: (T) -> Int,
): List<T> = sortedWith(compareBy<T> { if (isStarred(it)) 0 else 1 }.thenBy(baseIndex))

internal fun EpisodeDubbingsState.DubbingItem.withDubbingStar(
    starred: Set<String>,
): EpisodeDubbingsState.DubbingItem =
    copy(isStarred = name.isStarredDubbing(starred))

/** Расставляет звёзды в списке озвучек и поднимает помеченные наверх; остальные — в исходном порядке. */
internal fun List<EpisodeDubbingsState.DubbingItem>.withDubbingStars(
    starred: Set<String>,
): List<EpisodeDubbingsState.DubbingItem> =
    map { it.withDubbingStar(starred) }.starredFirstThenBase({ it.isStarred }, { it.baseIndex })

internal fun DubbingPickerState.withStars(starred: Set<String>): DubbingPickerState =
    copy(
        options = options
            .map { it.copy(item = it.item.withDubbingStar(starred)) }
            .starredFirstThenBase({ it.item.isStarred }, { it.item.baseIndex })
            .toImmutableList(),
    )

internal fun EpisodesState.EpisodeDubbingSelection.withStars(
    starred: Set<String>,
): EpisodesState.EpisodeDubbingSelection =
    copy(
        options = options
            .map { it.copy(item = it.item.withDubbingStar(starred)) }
            .starredFirstThenBase({ it.item.isStarred }, { it.item.baseIndex })
            .toImmutableList(),
    )

internal fun BalancerPickerState.withStars(starred: Set<String>): BalancerPickerState =
    copy(options = options.withBalancerStars(starred))

private fun ImmutableList<BalancerOption>.withBalancerStars(starred: Set<String>): ImmutableList<BalancerOption> =
    map { it.copy(isStarred = it.playerName.isStarredBalancer(starred)) }
        .starredFirstThenBase({ it.isStarred }, { it.baseIndex })
        .toImmutableList()
