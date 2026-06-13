package su.afk.yummy.tv.feature.details.details

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.usecase.GetVideoSubscriptionsUseCase
import su.afk.yummy.tv.domain.account.usecase.SetVideoSubscriptionUseCase
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.utils.SUBSCRIPTION_REFRESH_DELAY_MS
import su.afk.yummy.tv.feature.details.utils.matchesCurrentAnime
import su.afk.yummy.tv.feature.details.utils.toSubscriptionOptions
import javax.inject.Inject

/** Loads and mutates video subscription options shared by details and subscriptions screens. */
internal class DetailsSubscriptionHandler @Inject constructor(
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val settingsStore: SettingsStore,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val getVideoSubscriptions: GetVideoSubscriptionsUseCase,
    private val setVideoSubscription: SetVideoSubscriptionUseCase,
) {
    suspend fun loadScreenSubscriptionBase(
        animeId: Int,
        optimisticKeys: Set<String>,
    ): ScreenSubscriptionBaseResult {
        val token = yaniAuthPreferences.refreshToken.first()
        val userId = settingsStore.yaniUserId.first()
        if (token.isBlank() || userId <= 0) return ScreenSubscriptionBaseResult.SignedOut

        val details = runCatching { getAnimeDetails(animeId) }.getOrNull()
        return runCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                ScreenSubscriptionBaseResult.Content(
                    ScreenSubscriptionBase(
                        userId = userId,
                        details = details,
                        videos = videos,
                        subscriptions = videos.toSubscriptionOptions(optimisticKeys = optimisticKeys),
                    )
                )
            },
            onFailure = { error -> ScreenSubscriptionBaseResult.Failure(error.message) },
        )
    }

    suspend fun loadDetailsSubscriptions(
        animeId: Int,
        details: AnimeDetails?,
        videos: List<AnimeVideo>,
        userId: Int,
        optimisticKeys: Set<String>,
    ): Result<List<SubscriptionOption>> =
        loadRemoteSubscriptions(
            animeId = animeId,
            details = details,
            videos = videos,
            userId = userId,
            optimisticKeys = optimisticKeys,
        )

    suspend fun commitSubscriptionChange(videoId: Int, subscribed: Boolean): Boolean {
        val result = runCatching { setVideoSubscription(videoId, subscribed) }
        if (result.isSuccess) delay(SUBSCRIPTION_REFRESH_DELAY_MS)
        return result.isSuccess
    }

    private suspend fun loadRemoteSubscriptions(
        animeId: Int,
        details: AnimeDetails?,
        videos: List<AnimeVideo>,
        userId: Int,
        optimisticKeys: Set<String>,
    ): Result<List<SubscriptionOption>> =
        runCatching {
            val animeSubscriptions = getVideoSubscriptions(userId)
                .filter { it.matchesCurrentAnime(requestedAnimeId = animeId, details = details) }
            videos.toSubscriptionOptions(
                remoteSubscriptions = animeSubscriptions,
                optimisticKeys = optimisticKeys,
            )
        }
}

/** Base subscription screen data loaded before remote subscription state is merged. */
internal data class ScreenSubscriptionBase(
    val userId: Int,
    val details: AnimeDetails?,
    val videos: List<AnimeVideo>,
    val subscriptions: List<SubscriptionOption>,
)

/** Result of loading base subscription screen data. */
internal sealed interface ScreenSubscriptionBaseResult {
    data object SignedOut : ScreenSubscriptionBaseResult
    data class Content(val base: ScreenSubscriptionBase) : ScreenSubscriptionBaseResult
    data class Failure(val message: String?) : ScreenSubscriptionBaseResult
}
