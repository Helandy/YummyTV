package su.afk.yummy.tv.feature.account.userprofile.handler

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.usecase.GetUserAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserFavoriteAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserProfileSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserStatsUseCase
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState
import javax.inject.Inject

/** Fetches the profile overview (summary + stats) and the user's anime list tabs. */
internal class UserProfileContentHandler @Inject constructor(
    private val getUserProfileSummary: GetUserProfileSummaryUseCase,
    private val getUserStats: GetUserStatsUseCase,
    private val getUserFavoriteAnimeList: GetUserFavoriteAnimeListUseCase,
    private val getUserAnimeList: GetUserAnimeListUseCase,
) {
    suspend fun loadOverview(userId: Int): Result<Pair<UserProfileSummary, UserStats>> =
        runSuspendCatching {
            coroutineScope {
                val profile = async { getUserProfileSummary(userId) }
                val stats = async { getUserStats(userId) }
                profile.await() to stats.await()
            }
        }

    suspend fun loadLists(
        userId: Int,
        filter: UserProfileState.ListFilter,
        force: Boolean,
    ): Result<List<UserAnimeListItem>> =
        runSuspendCatching {
            if (filter == UserProfileState.ListFilter.FAVORITES) {
                getUserFavoriteAnimeList(userId, forceRefresh = force)
            } else {
                getUserAnimeList(userId, requireNotNull(filter.list), forceRefresh = force)
            }
        }
}
