package su.afk.yummy.tv.android.lifecycle

import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import su.afk.yummy.tv.android.lifecycle.OnlineStatusCoordinator.Companion.HEARTBEAT_INTERVAL_MS
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.utils.coroutines.di.DefaultApplicationScope
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.utils.system.DeviceHashProvider
import su.afk.yummy.tv.core.utils.system.ProcessLifecycleCoordinator
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.UpdateOnlineStatusUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Шлёт heartbeat "пользователь онлайн" на бэкенд, пока приложение видимо И пользователь
 * авторизован — обе части объединены в [combine], чтобы не пинговать бэкенд впустую для
 * анонимных сессий или пока приложение свёрнуто.
 *
 * [collectLatest] — ключевой механизм: любое изменение объединённого флага (свернули
 * приложение, разлогинились) отменяет текущий цикл heartbeat, а не просто выставляет флаг;
 * при возврате в активное состояние статус отправляется сразу и далее каждые
 * [HEARTBEAT_INTERVAL_MS].
 *
 * Идентификатор — хеш устройства ([DeviceHashProvider]), а не id аккаунта: бэкенду для этого
 * пинга достаточно знать, что конкретное устройство активно, привязка к личности не нужна.
 */
@Singleton
class OnlineStatusCoordinator @Inject constructor(
    private val deviceHashProvider: DeviceHashProvider,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val updateOnlineStatus: UpdateOnlineStatusUseCase,
    private val analyticsTracker: AnalyticsTracker,
    // Живёт весь процесс, как и observer ниже — отдельного cancel() нет, это осознанно.
    @DefaultApplicationScope private val scope: CoroutineScope,
) : ProcessLifecycleCoordinator() {
    private val isForeground = MutableStateFlow(false)

    // start() при двойном вызове заново запустил бы второй параллельный heartbeat-цикл.
    private var started = false

    override fun start() {
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
                .collectLatest { active ->
                    if (!active) return@collectLatest
                    while (isActive) {
                        sendOnlineStatus()
                        delay(HEARTBEAT_INTERVAL_MS)
                    }
                }
        }
        super.start()
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
            analyticsTracker.log(TAG) { "Skipping online status update: ANDROID_ID is unavailable" }
            return
        }

        runSuspendCatching { updateOnlineStatus(deviceHash) }
            .onFailure { error -> analyticsTracker.log(TAG, error) { "Failed to update online status" } }
    }

    private companion object {
        const val TAG = "OnlineStatus"
        const val HEARTBEAT_INTERVAL_MS = 3 * 60 * 1000L
    }
}
