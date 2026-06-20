package su.afk.yummy.tv.core.featuretoggle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class VarioqubFeatureToggleState @Inject constructor() : FeatureToggleUpdateObserver {

    private val initialized = AtomicBoolean(false)
    private val activationId = AtomicLong(0L)
    private val updateEvents = MutableSharedFlow<Long>(
        replay = 1,
        extraBufferCapacity = 1,
    )

    val isInitialized: Boolean
        get() = initialized.get()

    override val currentActivationId: Long
        get() = activationId.get()

    override val updates: Flow<Long> = updateEvents.asSharedFlow()

    fun markInitialized() {
        initialized.set(true)
    }

    fun markNotInitialized() {
        initialized.set(false)
    }

    fun notifyConfigActivated() {
        updateEvents.tryEmit(activationId.incrementAndGet())
    }
}
