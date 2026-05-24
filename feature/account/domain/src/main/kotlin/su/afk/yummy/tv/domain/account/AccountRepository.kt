package su.afk.yummy.tv.domain.account

interface AccountRepository {
    suspend fun login(login: String, password: String): YaniAccount
    suspend fun refreshToken(): YaniAccount?
    suspend fun getProfile(): YaniAccount
    suspend fun logout()
}

interface UserListsRepository {
    suspend fun getUserList(userId: Int, list: UserAnimeList): List<UserAnimeListItem>
    suspend fun getAnimeListState(animeId: Int): UserAnimeListItem?
    suspend fun setAnimeList(animeId: Int, list: UserAnimeList)
    suspend fun removeAnimeList(animeId: Int)
    suspend fun setFavorite(animeId: Int, favorite: Boolean)
}

interface VideoWatchesRepository {
    suspend fun markWatched(videoId: Int, timeSeconds: Int, durationSeconds: Int): Boolean
    suspend fun removeWatched(videoId: Int): Boolean
    suspend fun syncWatched(states: List<RemoteWatchState>): Boolean
}

interface AnimeExtrasRepository {
    suspend fun getRatingSummary(animeId: Int): AnimeRatingSummary
    suspend fun setRating(animeId: Int, rating: Int)
    suspend fun deleteRating(animeId: Int)
    suspend fun getListStats(animeId: Int): AnimeListStats
    suspend fun getCollections(animeId: Int, limit: Int = 20, offset: Int = 0): List<AnimeCollectionSummary>
    suspend fun getCollections(limit: Int = 40, offset: Int = 0): List<AnimeCollectionSummary>
}

interface VideoSubscriptionRepository {
    suspend fun setSubscribed(videoId: Int, subscribed: Boolean): Boolean
}

class LoginUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(login: String, password: String): YaniAccount = repository.login(login, password)
}

class LogoutUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke() = repository.logout()
}

class RefreshAccountUseCase(private val repository: AccountRepository) {
    suspend operator fun invoke(): YaniAccount? = repository.refreshToken()
}
