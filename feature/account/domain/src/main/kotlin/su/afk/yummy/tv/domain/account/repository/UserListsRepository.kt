package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem

interface UserListsRepository {
    suspend fun getUserList(
        userId: Int,
        list: UserAnimeList,
        forceRefresh: Boolean = false,
    ): List<UserAnimeListItem>

    suspend fun getUserFavorites(
        userId: Int,
        forceRefresh: Boolean = false,
    ): List<UserAnimeListItem>

    suspend fun hasCachedUserLists(userId: Int): Boolean

    suspend fun getAnimeListState(animeId: Int): UserAnimeListItem?
    suspend fun setAnimeList(animeId: Int, list: UserAnimeList)
    suspend fun removeAnimeList(animeId: Int)
    suspend fun setFavorite(animeId: Int, favorite: Boolean)
}
