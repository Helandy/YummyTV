package su.afk.yummy.tv.core.analytics.appmetrica

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Признак того, что AppMetrica активирована в этом процессе.
 *
 * SDK не умеет сообщать об активации, а вызов `reportEvent`/`reportError` до `activate`
 * бросает `ValidationException`. При пустом ключе активации нет, поэтому все обращения
 * к SDK проходят через этот флаг.
 */
@Singleton
internal class AppMetricaActivationState @Inject constructor() {

    @Volatile
    var isActivated: Boolean = false
        private set

    fun markActivated() {
        isActivated = true
    }
}
