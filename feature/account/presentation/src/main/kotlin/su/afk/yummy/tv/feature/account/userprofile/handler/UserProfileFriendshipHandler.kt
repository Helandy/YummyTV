package su.afk.yummy.tv.feature.account.userprofile.handler

import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.model.FriendshipStatus
import su.afk.yummy.tv.domain.account.usecase.AddFriendUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.GetFriendshipUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveFriendUseCase
import javax.inject.Inject

/** Resolves the viewer's relationship to a profile and performs friend/unfriend mutations. */
internal class UserProfileFriendshipHandler @Inject constructor(
    private val getAccountSession: GetAccountSessionUseCase,
    private val getFriendship: GetFriendshipUseCase,
    private val addFriend: AddFriendUseCase,
    private val removeFriend: RemoveFriendUseCase,
) {
    suspend fun resolveOwnership(userId: Int): FriendshipOwnership {
        val session = getAccountSession()
        return FriendshipOwnership(
            isAuthorized = session.isAuthorized,
            isOwnProfile = session.isAuthorized && session.userId == userId,
            sessionUserId = session.userId,
        )
    }

    suspend fun fetchStatus(sessionUserId: Int, userId: Int): FriendshipFetchResult =
        runSuspendCatching { getFriendship(sessionUserId, userId) }.fold(
            onSuccess = { status -> FriendshipFetchResult.Success(status) },
            onFailure = { FriendshipFetchResult.Failure },
        )

    suspend fun updateFriendship(
        sessionUserId: Int,
        userId: Int,
        currentStatus: FriendshipStatus,
    ): FriendshipFetchResult {
        val mutation = when (currentStatus) {
            FriendshipStatus.NONE,
            FriendshipStatus.FOLLOWERS,
            FriendshipStatus.REQUESTS -> suspend { addFriend(sessionUserId, userId) }

            FriendshipStatus.FRIENDS,
            FriendshipStatus.FOLLOWING,
            FriendshipStatus.SENT_REQUESTS -> suspend { removeFriend(sessionUserId, userId) }
        }
        return runSuspendCatching {
            mutation()
            getFriendship(sessionUserId, userId)
        }.fold(
            onSuccess = { status -> FriendshipFetchResult.Success(status) },
            onFailure = { FriendshipFetchResult.Failure },
        )
    }
}

/** The viewer's authorization/ownership state relative to a profile. */
internal data class FriendshipOwnership(
    val isAuthorized: Boolean,
    val isOwnProfile: Boolean,
    val sessionUserId: Int,
)

/** Outcome of fetching or mutating a friendship status. */
internal sealed interface FriendshipFetchResult {
    data class Success(val status: FriendshipStatus) : FriendshipFetchResult
    data object Failure : FriendshipFetchResult
}
