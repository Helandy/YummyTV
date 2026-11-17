package su.afk.yummy.tv.core.network.yani

import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Метка в контексте корутины: [YaniMirrorFallback.call] ставит [fromMirror], если ответ пришёл из резервной копии YummyTV, а не от yani.
 * Репозитории по ней решают, можно ли сохранять ответ поверх уже лежащего в кэше (копия беднее: у неё нет пользовательских полей, а
 * без полной копии деталей приходит только минимум). Работает без смены сигнатур API-классов.
 */
class MirrorTrace : AbstractCoroutineContextElement(Key) {
    @Volatile
    var fromMirror: Boolean = false

    companion object Key : CoroutineContext.Key<MirrorTrace>
}

/** Результат [withMirrorTrace]: [value] и признак того, что он получен из копии. */
data class Traced<T>(val value: T, val fromMirror: Boolean)

/** Выполняет [block] и сообщает, пришёл ли хоть один из его запросов к yani из резервной копии. */
suspend fun <T> withMirrorTrace(block: suspend () -> T): Traced<T> {
    val trace = MirrorTrace()
    val value = withContext(trace) { block() }
    return Traced(value, trace.fromMirror)
}

/**
 * Новый ответ не должен заменять уже сохранённые данные, потому что беднее их: ответ резервной копии (у неё нет части полей) или
 * пустой ответ там, где пустым он быть не может. Бросается из `fetchAndSave`, и `offlineFirstCache` и прочие обёртки отдают
 * сохранённое, как при сетевой ошибке.
 */
class MirrorDataSkippedException : RuntimeException("Новый ответ беднее сохранённых данных")
