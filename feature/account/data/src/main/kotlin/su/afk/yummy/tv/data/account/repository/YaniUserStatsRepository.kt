package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.account.mapper.toAnimeTypeStat
import su.afk.yummy.tv.data.account.mapper.toGenreStat
import su.afk.yummy.tv.data.account.mapper.toListWatchStat
import su.afk.yummy.tv.data.account.mapper.toRatingStat
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.repository.UserStatsRepository

class YaniUserStatsRepository(
    private val api: YaniAccountApi,
) : UserStatsRepository {
    override suspend fun getUserStats(userId: Int): UserStats = withContext(Dispatchers.IO) {
        val genres = async { api.getUserStatsGenres(userId).map { it.toGenreStat() } }
        val ratings = async { api.getUserStatsRatings(userId).map { it.toRatingStat() } }
        val lists = async { api.getUserStatsLists(userId).mapNotNull { it.toListWatchStat() } }
        val types = async { api.getUserStatsTypes(userId).mapNotNull { it.toAnimeTypeStat() } }

        UserStats(
            genres = genres.await(),
            ratings = ratings.await(),
            lists = lists.await(),
            types = types.await(),
        )
    }
}
