package su.afk.yummy.tv.core.utils.system

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Базовый класс для singleton-объектов, которым нужно реагировать на переход всего приложения
 * (не отдельной Activity) на передний план/в фон — например, обновлять фиче-тогглы или слать
 * heartbeat, пока приложение видимо пользователю.
 *
 * [ProcessLifecycleOwner] не подписывает наблюдателей сам — [start] нужно вызвать явно один раз
 * при старте процесса (см. `AppStartupRunner` в :app).
 */
abstract class ProcessLifecycleCoordinator : DefaultLifecycleObserver {

    open fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
}
