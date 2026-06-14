package su.afk.yummy.tv.core.analytics

interface AnalyticsContext {
    val params: Map<String, String>

    fun setParam(key: String, value: String?)
}

internal class DefaultAnalyticsContext : AnalyticsContext {
    private val lock = Any()
    private var contextParams: Map<String, String> = emptyMap()

    override val params: Map<String, String>
        get() = synchronized(lock) { contextParams }

    override fun setParam(key: String, value: String?) {
        val normalizedKey = key.trim()
        if (normalizedKey.isBlank()) return

        synchronized(lock) {
            contextParams = if (value.isNullOrBlank()) {
                contextParams - normalizedKey
            } else {
                contextParams + (normalizedKey to value)
            }
        }
    }
}
