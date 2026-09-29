package su.afk.yummy.tv.core.analytics.api

/**
 * Provides the device identifier assigned by the analytics provider.
 */
interface AnalyticsDeviceIdProvider {
    /**
     * Returns the analytics device identifier, or null when the provider is not activated
     * or failed to obtain it.
     */
    suspend fun deviceId(): String?
}
