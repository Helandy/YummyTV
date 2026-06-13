package su.afk.yummy.tv.core.analytics

data class AnalyticsEvent(
    val name: String,
    val params: Map<String, String> = emptyMap(),
)
