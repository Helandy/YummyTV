package su.afk.yummy.tv.core.common.coroutines

/** Источник текущего времени: внедряется вместо `System.currentTimeMillis()`, чтобы время можно было подменить. */
interface AppClock {
    fun nowMillis(): Long
}
