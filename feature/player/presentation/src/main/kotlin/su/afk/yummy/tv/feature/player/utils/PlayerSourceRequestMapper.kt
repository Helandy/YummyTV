package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.domain.player.model.PlayerSourceRequest
import su.afk.yummy.tv.feature.player.PlayerState

/** Собирает domain-запрос источников из текущего выбранного presentation-источника. */
internal fun PlayerState.State.toSourceRequest(): PlayerSourceRequest =
    PlayerSourceRequest(
        animeId = animeId,
        iframeUrl = activeIframeUrl(this),
        animeTitle = animeTitle,
        episode = activeEpisode(this),
        playerName = activeBalancerName(this),
        dubbing = activeDubbingName(this),
        selectedVideoId = activeVideoId(this),
        selectedPlayerId = activePlayerId(this),
        selectedScreenshotUrl = activeScreenshotUrl(this),
    )
