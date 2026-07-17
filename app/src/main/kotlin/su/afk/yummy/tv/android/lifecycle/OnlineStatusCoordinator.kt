package su.afk.yummy.tv.android.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.logger.AppLogger
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.UpdateOnlineStatusUseCase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineStatusCoordinator @Inject constructor(
    private val deviceHashProvider: DeviceHashProvider,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val updateOnlineStatus: UpdateOnlineStatusUseCase,
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val isForeground = MutableStateFlow(false)
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            combine(
                isForeground,
                observeAccountSession(),
            ) { foreground, session ->
                foreground && session.isAuthorized
            }
                .distinctUntilChanged()
                .filter { it }
                .collect { sendOnlineStatus() }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        isForeground.value = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isForeground.value = false
    }

    private suspend fun sendOnlineStatus() {
        val deviceHash = deviceHashProvider.get()
        if (deviceHash == null) {
            AppLogger.w(TAG) { "Skipping online status update: ANDROID_ID is unavailable" }
            return
        }

        try {
            updateOnlineStatus(deviceHash)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            AppLogger.w(TAG, error) { "Failed to update online status" }
        }
    }

    private companion object {
        const val TAG = "OnlineStatus"
    }
}
