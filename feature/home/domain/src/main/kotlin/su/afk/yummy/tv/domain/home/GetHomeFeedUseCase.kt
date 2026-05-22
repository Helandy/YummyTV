package su.afk.yummy.tv.domain.home

class GetHomeFeedUseCase(
    private val homeFeedRepository: HomeFeedRepository,
) {
    suspend operator fun invoke(): HomeFeed = homeFeedRepository.getHomeFeed()
}
