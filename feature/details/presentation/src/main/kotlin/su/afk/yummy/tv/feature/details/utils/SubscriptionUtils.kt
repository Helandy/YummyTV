package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.utils.removeHtmlEntities
import su.afk.yummy.tv.core.utils.stripHtmlTags
import su.afk.yummy.tv.domain.account.model.VideoSubscription
import su.afk.yummy.tv.feature.details.details.SubscriptionOption
import kotlin.time.Duration.Companion.milliseconds

internal val SUBSCRIPTION_REFRESH_DELAY = 350.milliseconds

internal fun List<AnimeVideo>.toSubscriptionOptions(
    remoteSubscriptions: List<VideoSubscription> = emptyList(),
    optimisticKeys: Set<String> = emptySet(),
    optimisticStates: Map<String, Boolean> = emptyMap(),
): List<SubscriptionOption> {
    val sortedOptions = filter { it.id > 0 && it.dubbing.isNotBlank() }
        .groupBy { subscriptionGroupKey(it.playerId, it.player, it.dubbing) }
        .values
        .mapNotNull { videos ->
            val representative = videos.maxWithOrNull(
                compareBy<AnimeVideo> { it.episode.toIntOrNull() ?: 0 }
                    .thenBy { it.id }
            ) ?: return@mapNotNull null
            val matchKeys = subscriptionMatchKeys(
                playerId = representative.playerId,
                player = representative.player,
                dubbing = representative.dubbing,
            )
            val optimisticState = matchKeys.optimisticSubscriptionState(optimisticStates)
            SubscriptionOptionWithViews(
                option = SubscriptionOption(
                    key = subscriptionGroupKey(
                        representative.playerId,
                        representative.player,
                        representative.dubbing
                    ),
                    playerId = representative.playerId,
                    player = representative.player,
                    dubbing = representative.dubbing,
                    episodesCount = videos.size,
                    representativeVideoId = representative.id,
                    isSubscribed = optimisticState
                        ?: (matchKeys.any { it in optimisticKeys } ||
                                remoteSubscriptions.any { it.matchesExactSubscription(representative) }),
                ),
                totalViews = videos.sumOf { it.views ?: 0 },
            )
        }
        .groupBy { it.option.dubbing.trim().lowercase() }
        .values
        .sortedByDescending { group -> group.sumOf { it.totalViews } }
        .flatMap { group ->
            group.sortedWith(
                compareByDescending<SubscriptionOptionWithViews> { it.totalViews }
                    .thenBy { it.option.player }
            )
        }
    val blankDubbingFallbackKeys = remoteSubscriptions
        .filter { it.dubbing.isBlank() }
        .mapNotNull { subscription ->
            sortedOptions.firstOrNull { subscription.matchesPlayer(it.option) }?.option?.key
        }
        .toSet()

    return sortedOptions.map { item ->
        val option = item.option
        val optimisticState =
            option.subscriptionMatchKeys().optimisticSubscriptionState(optimisticStates)
        if (option.key in blankDubbingFallbackKeys && optimisticState != false) {
            option.copy(isSubscribed = true)
        } else {
            option
        }
    }
}

private data class SubscriptionOptionWithViews(
    val option: SubscriptionOption,
    val totalViews: Int,
)

internal fun List<SubscriptionOption>.subscribedKeys(): Set<String> =
    filter { it.isSubscribed }
        .flatMap { it.subscriptionMatchKeys() }
        .toSet()

internal fun SubscriptionOption.subscriptionMatchKeys(): Set<String> =
    subscriptionMatchKeys(playerId = playerId, player = player, dubbing = dubbing)

private fun VideoSubscription.subscriptionMatchKeys(): Set<String> =
    subscriptionMatchKeys(playerId = playerId, player = player, dubbing = dubbing)

internal fun VideoSubscription.matchesCurrentAnime(
    requestedAnimeId: Int,
    details: AnimeDetails?,
): Boolean {
    if (animeId == requestedAnimeId || animeId == details?.id) return true
    val detailsAnimeUrl = details?.animeUrl.orEmpty()
    return animeUrl.isNotBlank() && detailsAnimeUrl.isNotBlank() && animeUrl == detailsAnimeUrl
}

private fun VideoSubscription.matchesExactSubscription(video: AnimeVideo): Boolean {
    if (dubbing.isBlank()) return false
    val dubbingMatches =
        dubbing.relaxedSubscriptionPart().matchesRelaxed(video.dubbing.relaxedSubscriptionPart())
    if (!dubbingMatches) return false

    return matchesPlayer(
        playerId = video.playerId,
        player = video.player,
    )
}

private fun VideoSubscription.matchesPlayer(option: SubscriptionOption): Boolean =
    matchesPlayer(playerId = option.playerId, player = option.player)

private fun VideoSubscription.matchesPlayer(playerId: Int?, player: String): Boolean {
    val playerIdMatches = this.playerId != null && playerId != null && this.playerId == playerId
    if (playerIdMatches) return true

    val playerMatches =
        this.player.relaxedSubscriptionPart().matchesRelaxed(player.relaxedSubscriptionPart())
    if (playerMatches) return true

    return this.playerId == null && this.player.isBlank()
}

private fun subscriptionMatchKeys(playerId: Int?, player: String, dubbing: String): Set<String> =
    buildSet {
        val normalizedDubbing = dubbing.normalizedSubscriptionPart()
        if (normalizedDubbing.isBlank()) return@buildSet
        if (playerId != null) add("playerId:$playerId|dubbing:$normalizedDubbing")
        val normalizedPlayer = player.normalizedSubscriptionPart()
        if (normalizedPlayer.isNotBlank()) add("player:$normalizedPlayer|dubbing:$normalizedDubbing")
    }

private fun Set<String>.optimisticSubscriptionState(optimisticStates: Map<String, Boolean>): Boolean? =
    firstNotNullOfOrNull { optimisticStates[it] }

private fun subscriptionGroupKey(playerId: Int?, player: String, dubbing: String): String {
    val normalizedDubbing = dubbing.normalizedSubscriptionPart()
    val normalizedPlayer = player.normalizedSubscriptionPart()
    return "playerId:${playerId ?: -1}|player:$normalizedPlayer|dubbing:$normalizedDubbing"
}

private fun String.normalizedSubscriptionPart(): String =
    trim().lowercase()

private fun String.relaxedSubscriptionPart(): String =
    trim()
        .lowercase()
        .replace('ё', 'е')
        .stripHtmlTags()
        .removeHtmlEntities()
        .filter { it.isLetterOrDigit() }

private fun String.matchesRelaxed(other: String): Boolean =
    isNotBlank() && other.isNotBlank() && (this == other || this.contains(other) || other.contains(
        this
    ))
