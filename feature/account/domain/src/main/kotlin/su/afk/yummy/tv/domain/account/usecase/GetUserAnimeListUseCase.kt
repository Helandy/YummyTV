package su.afk.yummy.tv.domain.account

/** Loads a user's anime list for the selected Yani list category. */
class GetUserAnimeListUseCase(private val repository: UserListsRepository) {
    suspend operator fun invoke(userId: Int, list: UserAnimeList): List<UserAnimeListItem> =
        repository.getUserList(userId, list)
}
