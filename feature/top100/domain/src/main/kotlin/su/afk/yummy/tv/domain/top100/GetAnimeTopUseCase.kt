package su.afk.yummy.tv.domain.top100

private const val BLOCKED_REGION = "RU"

class GetAnimeTopUseCase(
    private val repository: AnimeTopRepository,
    private val hideRegionBlocked: Boolean,
) {
    suspend operator fun invoke(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage {
        val page = repository.getTopAnime(type, limit, offset)
        return if (!hideRegionBlocked) {
            page
        } else {
            page.copy(items = page.items.filterNot { it.isBlockedInRegion() })
        }
    }

    private fun AnimeTopItem.isBlockedInRegion(): Boolean =
        blockedIn.any { it.equals(BLOCKED_REGION, ignoreCase = true) }
}
