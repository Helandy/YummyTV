package su.afk.yummy.tv.core.analytics

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}
