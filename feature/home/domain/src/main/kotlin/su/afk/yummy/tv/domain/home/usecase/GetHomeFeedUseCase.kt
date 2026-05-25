package su.afk.yummy.tv.domain.home

/** Loads the home feed sections shown on the main screen. */
class GetHomeFeedUseCase(
    private val homeFeedRepository: HomeFeedRepository,
) {
    suspend operator fun invoke(): HomeFeed = homeFeedRepository.getHomeFeed()
}
