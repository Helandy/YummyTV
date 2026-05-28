package su.afk.yummy.tv.data.account.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.account.mapper.toUserAnimeList
import su.afk.yummy.tv.data.account.mapper.toUserListItem
import su.afk.yummy.tv.data.account.network.YaniAccountApi
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.repository.UserListsRepository

class YaniUserListsRepository(
    private val api: YaniAccountApi,
) : UserListsRepository {

    override suspend fun getUserList(userId: Int, list: UserAnimeList): List<UserAnimeListItem> =
        withContext(Dispatchers.IO) { api.getUserList(userId, list.id).mapNotNull { it.toUserListItem() } }

    override suspend fun getUserFavorites(userId: Int): List<UserAnimeListItem> =
        withContext(Dispatchers.IO) { api.getUserList(userId, FAVORITES_LIST_ID).mapNotNull { it.toUserListItem() } }

    override suspend fun getAnimeListState(animeId: Int): UserAnimeListItem? = withContext(Dispatchers.IO) {
        val state = api.getAnimeListState(animeId)
        UserAnimeListItem(
            animeId = animeId,
            title = "",
            posterUrl = null,
            rating = null,
            year = null,
            list = state.list.toUserAnimeList(),
            isFavorite = state.isFavorite,
        )
    }

    override suspend fun setAnimeList(animeId: Int, list: UserAnimeList) = withContext(Dispatchers.IO) {
        api.setAnimeList(animeId, list.id)
    }

    override suspend fun removeAnimeList(animeId: Int) = withContext(Dispatchers.IO) {
        api.removeAnimeList(animeId)
    }

    override suspend fun setFavorite(animeId: Int, favorite: Boolean) = withContext(Dispatchers.IO) {
        if (favorite) {
            api.setFavorite(animeId)
        } else {
            api.removeFavorite(animeId)
        }
    }

    private companion object {
        const val FAVORITES_LIST_ID = 4
    }
}
