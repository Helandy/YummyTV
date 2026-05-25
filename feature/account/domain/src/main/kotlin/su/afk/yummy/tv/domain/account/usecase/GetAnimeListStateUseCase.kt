package su.afk.yummy.tv.domain.account

/** Loads the current user's list state for a single anime. */
class GetAnimeListStateUseCase(private val repository: UserListsRepository) {
    suspend operator fun invoke(animeId: Int): UserAnimeListItem? = repository.getAnimeListState(animeId)
}
