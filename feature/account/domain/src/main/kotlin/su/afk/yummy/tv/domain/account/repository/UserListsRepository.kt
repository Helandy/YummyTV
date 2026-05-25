package su.afk.yummy.tv.domain.account

interface UserListsRepository {
    suspend fun getUserList(userId: Int, list: UserAnimeList): List<UserAnimeListItem>
    suspend fun getAnimeListState(animeId: Int): UserAnimeListItem?
    suspend fun setAnimeList(animeId: Int, list: UserAnimeList)
    suspend fun removeAnimeList(animeId: Int)
    suspend fun setFavorite(animeId: Int, favorite: Boolean)
}
