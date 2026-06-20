package su.afk.yummy.tv.core.featuretoggle

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class VarioqubFeatureToggleState @Inject constructor() {

    private val initialized = AtomicBoolean(false)

    val isInitialized: Boolean
        get() = initialized.get()

    fun markInitialized() {
        initialized.set(true)
    }

    fun markNotInitialized() {
        initialized.set(false)
    }
}
