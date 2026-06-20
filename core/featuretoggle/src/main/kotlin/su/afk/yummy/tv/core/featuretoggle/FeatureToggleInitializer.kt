package su.afk.yummy.tv.core.featuretoggle

import android.content.Context

interface FeatureToggleInitializer {
    fun initialize(context: Context, clientId: String)
}
