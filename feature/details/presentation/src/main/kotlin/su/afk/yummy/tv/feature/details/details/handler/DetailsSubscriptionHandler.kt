package su.afk.yummy.tv.feature.details.details.handler

import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.SetVideoSubscriptionUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.details.model.SubscriptionOption
import su.afk.yummy.tv.feature.details.mapper.toSubscriptionOptions
import su.afk.yummy.tv.feature.details.utils.SUBSCRIPTION_REFRESH_DELAY
import javax.inject.Inject
import javax.inject.Singleton

/** Loads and mutates video subscription options shared by details and subscriptions screens. */
@Singleton
internal class DetailsSubscriptionHandler @Inject constructor(
    private val getAccountSession: GetAccountSessionUseCase,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val refreshAnimeVideos: RefreshAnimeVideosUseCase,
    private val setVideoSubscription: SetVideoSubscriptionUseCase,
) {
    /**
     * Значения кнопок, пока запрос в полёте. Записи живут только на время мутации: дальше состояние
     * снова приходит от сервера полем `subscribed` в списке видео.
     */
    private var pendingStatesByAnimeId = emptyMap<Int, Map<String, Boolean>>()

    fun pendingSubscriptionStates(animeId: Int): Map<String, Boolean> =
        pendingStatesByAnimeId[animeId].orEmpty()

    suspend fun loadScreenSubscriptionBase(animeId: Int): ScreenSubscriptionBaseResult {
        val session = getAccountSession()
        if (!session.isAuthorized || session.userId <= 0) return ScreenSubscriptionBaseResult.SignedOut

        val details = runSuspendCatching { getAnimeDetails(animeId) }.getOrNull()
        return runSuspendCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                ScreenSubscriptionBaseResult.Content(
                    ScreenSubscriptionBase(
                        details = details,
                        subscriptions = videos.toSubscriptionOptions(
                            pendingStates = pendingSubscriptionStates(animeId),
                        ),
                    )
                )
            },
            onFailure = { error -> ScreenSubscriptionBaseResult.Failure(error.message, error) },
        )
    }

    /**
     * Перечитывает список видео с сервера: подписка меняется именно там, а кэш видео живёт 5 минут,
     * и без принудительного запроса галочка вернулась бы в прежнее состояние.
     */
    suspend fun reloadSubscriptions(animeId: Int): Result<List<SubscriptionOption>> =
        runSuspendCatching {
            refreshAnimeVideos(animeId).toSubscriptionOptions(
                pendingStates = pendingSubscriptionStates(animeId),
            )
        }

    suspend fun commitSubscriptionChange(
        animeId: Int,
        option: SubscriptionOption,
        subscribed: Boolean
    ): Boolean {
        setPendingState(animeId, option.key, subscribed)
        return try {
            val changed = runSuspendCatching {
                setVideoSubscription(option.subscriptionVideoId, subscribed)
            }.getOrDefault(false)
            if (changed) delay(SUBSCRIPTION_REFRESH_DELAY)
            changed
        } finally {
            clearPendingState(animeId, option.key)
        }
    }

    private fun setPendingState(animeId: Int, key: String, subscribed: Boolean) {
        val current = pendingStatesByAnimeId[animeId].orEmpty()
        pendingStatesByAnimeId += animeId to (current + (key to subscribed))
    }

    private fun clearPendingState(animeId: Int, key: String) {
        val current = pendingStatesByAnimeId[animeId].orEmpty() - key
        pendingStatesByAnimeId = if (current.isEmpty()) {
            pendingStatesByAnimeId - animeId
        } else {
            pendingStatesByAnimeId + (animeId to current)
        }
    }
}

/** Base subscription screen data. */
internal data class ScreenSubscriptionBase(
    val details: AnimeDetails?,
    val subscriptions: List<SubscriptionOption>,
)

/** Result of loading base subscription screen data. */
internal sealed interface ScreenSubscriptionBaseResult {
    data object SignedOut : ScreenSubscriptionBaseResult
    data class Content(val base: ScreenSubscriptionBase) : ScreenSubscriptionBaseResult
    data class Failure(val message: String?, val error: Throwable) : ScreenSubscriptionBaseResult
}
