package su.afk.yummy.tv.core.featuretoggle

import kotlinx.coroutines.flow.Flow

interface FeatureToggleUpdateObserver {
    val currentActivationId: Long
    val updates: Flow<Long>
}
