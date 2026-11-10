package su.afk.yummy.tv.core.common.player

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Реестр резервных узлов okcdn для прозрачного переезда на уровне HTTP-запроса.
 *
 * Подпись okcdn от узла не зависит (см. [withCdnHost]), поэтому запрос, который основной узел
 * отверг, можно повторить на резервном: плеер получит ответ и не заметит отказа. Реестр хранит
 * пары «основной узел → резервный», которые знает балансер CVH, и список узлов, отвечавших отказом
 * недавно: к ним не обращаемся первыми, чтобы каждый запрос не платил за заведомый отказ.
 *
 * Состояние общее на процесс, как у [AllohaEdgeHash]: запросы плеера идут из потока загрузчика,
 * где ViewModel недоступна.
 */
object OkCdnHostFailover {
    /** Сколько основной узел считается отказавшим, прежде чем его снова попробуют первым. */
    private val DEAD_TTL_NANOS = TimeUnit.MINUTES.toNanos(5)

    private val failoverByHost = ConcurrentHashMap<String, String>()
    private val deadSinceNanos = ConcurrentHashMap<String, Long>()

    /** Запоминает резервный узел для [primaryHost]. Пустые и совпадающие хосты игнорируются. */
    fun register(primaryHost: String, failoverHost: String) {
        val primary = primaryHost.trim().lowercase()
        val failover = failoverHost.trim().lowercase()
        if (primary.isEmpty() || failover.isEmpty() || primary == failover) return
        failoverByHost[primary] = failover
    }

    /** Резервный узел для [host] или null: пара не зарегистрирована. */
    fun failoverFor(host: String): String? = failoverByHost[host.lowercase()]

    /** Отмечает [host] как отказавший: следующие запросы пойдут сразу на резервный. */
    fun markDead(host: String, nowNanos: Long = System.nanoTime()) {
        deadSinceNanos[host.lowercase()] = nowNanos
    }

    /** Отказал ли [host] недавно. По истечении срока узел снова пробуют первым. */
    fun isDead(host: String, nowNanos: Long = System.nanoTime()): Boolean {
        val key = host.lowercase()
        val since = deadSinceNanos[key] ?: return false
        if (nowNanos - since < DEAD_TTL_NANOS) return true
        deadSinceNanos.remove(key, since)
        return false
    }
}
