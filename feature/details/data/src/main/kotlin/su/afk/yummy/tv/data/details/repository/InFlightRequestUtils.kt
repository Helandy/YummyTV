package su.afk.yummy.tv.data.details.repository

import kotlinx.coroutines.Deferred
import java.util.concurrent.ConcurrentHashMap

/** Возвращает общий запрос и удаляет его после завершения, не затрагивая более новый запрос с тем же ключом. */
internal fun <K : Any, V> ConcurrentHashMap<K, Deferred<V>>.getOrCreateInFlight(
    key: K,
    create: () -> Deferred<V>,
): Deferred<V> {
    val deferred = computeIfAbsent(key) { create() }
    // Уже завершённый Deferred вызывает обработчик синхронно. Регистрируем его после computeIfAbsent,
    // чтобы удаление не пыталось рекурсивно изменить эту же карту во время вычисления значения.
    deferred.invokeOnCompletion { remove(key, deferred) }
    return deferred
}
