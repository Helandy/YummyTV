package su.afk.yummy.tv.domain.account.mutation

import kotlinx.coroutines.flow.SharedFlow

enum class AccountMutationAction {
    SET_ANIME_LIST,
    REMOVE_ANIME_LIST,
    SET_FAVORITE,
    REMOVE_FAVORITE,
    SET_RATING,
    DELETE_RATING,
    SET_VIDEO_SUBSCRIPTION,
    REMOVE_VIDEO_SUBSCRIPTION,
    REMOVE_WATCHED,
    SYNC_WATCHED,
    MARK_NOTIFICATION_READ,
    MARK_ALL_NOTIFICATIONS_READ,
    DELETE_NOTIFICATION,
}

data class AccountMutationErrorEvent(
    val action: AccountMutationAction,
    val message: String?,
)

interface AccountMutationErrorNotifier {
    val events: SharedFlow<AccountMutationErrorEvent>

    suspend fun notify(event: AccountMutationErrorEvent)
}
