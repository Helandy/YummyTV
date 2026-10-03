package su.afk.yummy.tv.core.utils.coroutines

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemAppClock @Inject constructor() : AppClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
