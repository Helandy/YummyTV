package su.afk.yummy.tv.domain.anime

private const val BLOCKED_REGION = "RU"
private const val BLOCKED_TIMEOUT_MS = 6 * 60 * 60 * 1000L

class IsAnimeRegionBlockedUseCase(
    private val blockedTimeoutEnabled: Boolean,
    private val firstLaunchTimestampProvider: FirstLaunchTimestampProvider,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(details: AnimeDetails): Boolean {
        if (!blockedTimeoutEnabled) return false
        if (details.blockedIn.none { it.equals(BLOCKED_REGION, ignoreCase = true) }) return false

        val firstLaunchAt = firstLaunchTimestampProvider.getOrCreateFirstLaunchAtMillis()
        return nowMillis() - firstLaunchAt < BLOCKED_TIMEOUT_MS
    }
}
