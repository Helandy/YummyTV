package su.afk.yummy.tv.feature.details.details.handler

import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.GetVideoSubscriptionsUseCase
import su.afk.yummy.tv.domain.account.usecase.SetVideoSubscriptionUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.details.SubscriptionOption
import su.afk.yummy.tv.feature.details.utils.SUBSCRIPTION_REFRESH_DELAY
import su.afk.yummy.tv.feature.details.utils.matchesCurrentAnime
import su.afk.yummy.tv.feature.details.utils.subscriptionMatchKeys
import su.afk.yummy.tv.feature.details.utils.toSubscriptionOptions
import javax.inject.Inject
import javax.inject.Singleton

/** Loads and mutates video subscription options shared by details and subscriptions screens. */
@Singleton
internal class DetailsSubscriptionHandler @Inject constructor(
    private val getAccountSession: GetAccountSessionUseCase,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val getVideoSubscriptions: GetVideoSubscriptionsUseCase,
    private val setVideoSubscription: SetVideoSubscriptionUseCase,
) {
    private var optimisticSubscriptionStatesByAnimeId = emptyMap<Int, Map<String, Boolean>>()

    fun optimisticSubscriptionStates(animeId: Int): Map<String, Boolean> =
        optimisticSubscriptionStatesByAnimeId[animeId].orEmpty()

    fun updateOptimisticSubscriptionState(
        animeId: Int,
        option: SubscriptionOption,
        subscribed: Boolean,
    ) {
        val currentStates = optimisticSubscriptionStatesByAnimeId[animeId].orEmpty()
        optimisticSubscriptionStatesByAnimeId += animeId to (
                currentStates + option.subscriptionMatchKeys().associateWith { subscribed }
                )
    }

    suspend fun loadScreenSubscriptionBase(
        animeId: Int,
        optimisticKeys: Set<String>,
        optimisticStates: Map<String, Boolean> = emptyMap(),
    ): ScreenSubscriptionBaseResult {
        val session = getAccountSession()
        if (!session.isAuthorized || session.userId <= 0) return ScreenSubscriptionBaseResult.SignedOut

        val details = runCatching { getAnimeDetails(animeId) }.getOrNull()
        return runCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                ScreenSubscriptionBaseResult.Content(
                    ScreenSubscriptionBase(
                        userId = session.userId,
                        details = details,
                        videos = videos,
                        subscriptions = videos.toSubscriptionOptions(
                            optimisticKeys = optimisticKeys,
                            optimisticStates = optimisticStates,
                        ),
                    )
                )
            },
            onFailure = { error -> ScreenSubscriptionBaseResult.Failure(error.message, error) },
        )
    }

    suspend fun loadDetailsSubscriptions(
        animeId: Int,
        details: AnimeDetails?,
        videos: List<AnimeVideo>,
        userId: Int,
        optimisticKeys: Set<String>,
        optimisticStates: Map<String, Boolean> = emptyMap(),
    ): Result<List<SubscriptionOption>> =
        loadRemoteSubscriptions(
            animeId = animeId,
            details = details,
            videos = videos,
            userId = userId,
            optimisticKeys = optimisticKeys,
            optimisticStates = optimisticStates,
        )

    suspend fun commitSubscriptionChange(videoId: Int, subscribed: Boolean): Boolean {
        val result = runCatching { setVideoSubscription(videoId, subscribed) }
        if (result.isSuccess) delay(SUBSCRIPTION_REFRESH_DELAY)
        return result.isSuccess
    }

    private suspend fun loadRemoteSubscriptions(
        animeId: Int,
        details: AnimeDetails?,
        videos: List<AnimeVideo>,
        userId: Int,
        optimisticKeys: Set<String>,
        optimisticStates: Map<String, Boolean>,
    ): Result<List<SubscriptionOption>> =
        runCatching {
            val animeSubscriptions = getVideoSubscriptions(userId)
                .filter { it.matchesCurrentAnime(requestedAnimeId = animeId, details = details) }
            videos.toSubscriptionOptions(
                remoteSubscriptions = animeSubscriptions,
                optimisticKeys = optimisticKeys,
                optimisticStates = optimisticStates,
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
    data class Failure(val message: String?, val error: Throwable) : ScreenSubscriptionBaseResult
}
