package su.afk.yummy.tv.core.analytics

interface AnalyticsDestination {
    val screenName: String
    val screenParams: Map<String, String>
        get() = emptyMap()
}
