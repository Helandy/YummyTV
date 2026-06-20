package su.afk.yummy.tv.core.featuretoggle

import su.afk.yummy.tv.core.logger.AppLogger
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class FeatureToggleVersionSupportChecker @Inject constructor(
    private val featureToggleProvider: FeatureToggleProvider,
    @param:Named("appVersionName") private val versionName: String,
) : VersionSupportChecker {

    override fun isCurrentVersionSupported(): Boolean {
        val minSupportedVersion =
            featureToggleProvider.getString(FeatureFlags.minSupportedAppVersion)
        val isSupported = isVersionSupported(
            currentVersion = versionName,
            minSupportedVersion = minSupportedVersion,
        )
        AppLogger.d(TAG) {
            "Version support current=$versionName minSupported=$minSupportedVersion supported=$isSupported"
        }
        return isSupported
    }

    private fun isVersionSupported(
        currentVersion: String,
        minSupportedVersion: String,
    ): Boolean {
        val minParts = minSupportedVersion.toVersionParts()
        if (minParts.isEmpty()) return true

        val currentParts = currentVersion.toVersionParts()
        if (currentParts.isEmpty()) return true

        val length = maxOf(currentParts.size, minParts.size)
        for (index in 0 until length) {
            val current = currentParts.getOrElse(index) { 0 }
            val min = minParts.getOrElse(index) { 0 }
            if (current > min) return true
            if (current < min) return false
        }
        return true
    }

    private fun String.toVersionParts(): List<Int> =
        trim()
            .removePrefix("v")
            .split(Regex("[^0-9]+"))
            .mapNotNull { it.toIntOrNull() }

    private companion object {
        const val TAG = "FeatureToggles"
    }
}
