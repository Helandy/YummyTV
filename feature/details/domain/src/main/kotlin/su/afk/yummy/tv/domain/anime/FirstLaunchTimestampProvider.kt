package su.afk.yummy.tv.domain.anime

interface FirstLaunchTimestampProvider {
    suspend fun getOrCreateFirstLaunchAtMillis(): Long
}
