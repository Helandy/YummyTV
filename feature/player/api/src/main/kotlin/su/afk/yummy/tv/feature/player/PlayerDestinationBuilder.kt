package su.afk.yummy.tv.feature.player

import androidx.navigation3.runtime.NavKey

data class PlayerVideoSource(
    val id: Int,
    val episode: String,
    val dubbing: String,
    val player: String,
    val iframeUrl: String,
    val views: Int? = null,
    val skips: PlayerSkips = PlayerSkips.Empty,
)

fun IPlayerNavigator.getPlayerDest(
    video: PlayerVideoSource,
    allVideos: List<PlayerVideoSource>,
    animeTitle: String,
    animeId: Int,
    posterUrl: String = "",
    screenshotByEpisode: Map<String, String> = emptyMap(),
): NavKey {
    val playerName = video.player
    val selectedDubbing = video.dubbing

    val dubbingGroups = allVideos
        .filter { it.player == playerName }
        .groupBy { it.dubbing }
        .mapValues { (_, videos) -> videos.sortedByEpisode() }
    val dubbingNames = dubbingGroups.keys.toList()
    val currentDubbingIndex = dubbingNames.indexOf(selectedDubbing).coerceAtLeast(0)
    val group = dubbingGroups[selectedDubbing].orEmpty()
    val currentEpisodeIndex = group.indexOfFirst { it.id == video.id }.coerceAtLeast(0)
    val allDubbingViews = dubbingNames.map { name -> dubbingGroups[name].orEmpty().sumViews() }

    val supportedBalancers = allVideos
        .map { it.player }
        .distinct()
        .filter { player -> allVideos.firstOrNull { it.player == player }?.iframeUrl?.isSupportedPlayerUrl() == true }
    val currentBalancerIndex = supportedBalancers.indexOf(playerName).coerceAtLeast(0)
    val allBalancerDubbingNames = supportedBalancers.map { balancer ->
        allVideos.filter { it.player == balancer }.map { it.dubbing }.distinct()
    }
    val allBalancerEpisodeUrls = supportedBalancers.mapIndexed { balancerIndex, balancer ->
        allBalancerDubbingNames[balancerIndex].map { dubbing ->
            allVideos.filter { it.player == balancer && it.dubbing == dubbing }
                .sortedByEpisode()
                .map { it.iframeUrl }
        }
    }
    val allBalancerEpisodeNumbers = supportedBalancers.mapIndexed { balancerIndex, balancer ->
        allBalancerDubbingNames[balancerIndex].map { dubbing ->
            allVideos.filter { it.player == balancer && it.dubbing == dubbing }
                .sortedByEpisode()
                .map { it.episode }
        }
    }
    val allBalancerEpisodeVideoIds = supportedBalancers.mapIndexed { balancerIndex, balancer ->
        allBalancerDubbingNames[balancerIndex].map { dubbing ->
            allVideos.filter { it.player == balancer && it.dubbing == dubbing }
                .sortedByEpisode()
                .map { it.id }
        }
    }
    val allBalancerEpisodeSkips = supportedBalancers.mapIndexed { balancerIndex, balancer ->
        allBalancerDubbingNames[balancerIndex].map { dubbing ->
            allVideos.filter { it.player == balancer && it.dubbing == dubbing }
                .sortedByEpisode()
                .map { it.skips }
        }
    }
    val allBalancerDubbingViews = supportedBalancers.mapIndexed { balancerIndex, balancer ->
        allBalancerDubbingNames[balancerIndex].map { dubbing ->
            allVideos.filter { it.player == balancer && it.dubbing == dubbing }.sumViews()
        }
    }
    val kodikIframeByEpisode = allVideos
        .filter { it.iframeUrl.isKodikPlayerUrl() }
        .groupBy { it.episode }
        .mapValues { (_, videos) -> videos.first().iframeUrl }

    return getPlayerDest(
        iframeUrl = video.iframeUrl,
        animeTitle = animeTitle,
        episode = video.episode,
        playerName = playerName,
        dubbing = selectedDubbing,
        episodeUrls = group.map { it.iframeUrl },
        episodeNumbers = group.map { it.episode },
        episodeVideoIds = group.map { it.id },
        currentEpisodeIndex = currentEpisodeIndex,
        screenshotUrls = group.map { kodikIframeByEpisode[it.episode] ?: screenshotByEpisode[it.episode].orEmpty() },
        animeId = animeId,
        posterUrl = posterUrl,
        allDubbingNames = dubbingNames,
        currentDubbingIndex = currentDubbingIndex,
        allDubbingEpisodeUrls = dubbingNames.map { name -> dubbingGroups[name].orEmpty().map { it.iframeUrl } },
        allDubbingEpisodeNumbers = dubbingNames.map { name -> dubbingGroups[name].orEmpty().map { it.episode } },
        allDubbingEpisodeVideoIds = dubbingNames.map { name -> dubbingGroups[name].orEmpty().map { it.id } },
        allDubbingViews = allDubbingViews,
        allBalancerNames = supportedBalancers,
        currentBalancerIndex = currentBalancerIndex,
        allBalancerDubbingNames = allBalancerDubbingNames,
        allBalancerEpisodeUrls = allBalancerEpisodeUrls,
        allBalancerEpisodeNumbers = allBalancerEpisodeNumbers,
        allBalancerEpisodeVideoIds = allBalancerEpisodeVideoIds,
        allBalancerDubbingViews = allBalancerDubbingViews,
        episodeSkips = group.map { it.skips },
        allDubbingEpisodeSkips = dubbingNames.map { name -> dubbingGroups[name].orEmpty().map { it.skips } },
        allBalancerEpisodeSkips = allBalancerEpisodeSkips,
    )
}

fun List<PlayerVideoSource>.selectContinueWatchingVideo(
    videoId: Int,
    episodeUrl: String,
    episode: String,
    playerName: String,
    dubbing: String,
): PlayerVideoSource? {
    val exact = firstOrNull { videoId > 0 && it.id == videoId }
        ?: firstOrNull { episodeUrl.isNotBlank() && it.iframeUrl == episodeUrl }
        ?: firstOrNull {
            !episode.isPlaceholderEpisode() &&
                it.episode == episode &&
                it.player == playerName &&
                it.dubbing == dubbing
        }
        ?: firstOrNull { !episode.isPlaceholderEpisode() && it.episode == episode }

    if (exact?.iframeUrl?.isSupportedPlayerUrl() == true) return exact

    val targetEpisode = exact?.episode?.takeUnless { it.isPlaceholderEpisode() }
        ?: episode.takeUnless { it.isPlaceholderEpisode() }
    val sameEpisode = targetEpisode?.let { ep -> filter { it.episode == ep } }.orEmpty()
    return sameEpisode.firstOrNull { it.iframeUrl.isSupportedPlayerUrl() }
        ?: firstOrNull { it.iframeUrl.isSupportedPlayerUrl() }
        ?: exact
}

fun String.isPlaceholderEpisode(): Boolean = trim().isEmpty() || trim() == "-"

private fun List<PlayerVideoSource>.sortedByEpisode(): List<PlayerVideoSource> =
    sortedBy { it.episode.toIntOrNull() ?: Int.MAX_VALUE }

private fun List<PlayerVideoSource>.sumViews(): Int = sumOf { it.views ?: 0 }

private fun String.isKodikPlayerUrl(): Boolean = contains("kodik", ignoreCase = true)

private fun String.isSupportedPlayerUrl(): Boolean =
    contains("kodik", ignoreCase = true) ||
        contains("aksor.tv", ignoreCase = true) ||
        contains("iframeCVH", ignoreCase = true) ||
        contains("alloha", ignoreCase = true)
