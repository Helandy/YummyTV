package su.afk.yummy.tv.domain.account.repository

import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem

interface UserListsRepository {
    suspend fun getUserList(userId: Int, list: UserAnimeList): List<UserAnimeListItem>
    suspend fun getUserFavorites(userId: Int): List<UserAnimeListItem>
    suspend fun getAnimeListState(animeId: Int): UserAnimeListItem?
    suspend fun setAnimeList(animeId: Int, list: UserAnimeList)
    suspend fun removeAnimeList(animeId: Int)
    suspend fun setFavorite(animeId: Int, favorite: Boolean)
}
