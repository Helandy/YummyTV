package su.afk.yummy.tv.domain.anime

private const val BLOCKED_REGION = "RU"

class IsAnimeRegionBlockedUseCase(
    private val hideRegionBlocked: Boolean,
) {
    operator fun invoke(details: AnimeDetails): Boolean =
        hideRegionBlocked && details.blockedIn.any { it.equals(BLOCKED_REGION, ignoreCase = true) }
}
