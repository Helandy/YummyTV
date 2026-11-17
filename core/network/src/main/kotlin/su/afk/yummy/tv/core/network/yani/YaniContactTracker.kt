package su.afk.yummy.tv.core.network.yani

import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Помнит, отвечал ли api.yani.tv в этом запуске приложения. Ответ любого статуса ниже 500 (в том числе 4xx и 401) значит, что yani
 * жив; сетевая ошибка, 5xx и ответы резервной копии не считаются. Плановая чистка кэша ждёт этого сигнала: пока yani
 * недоступен, кэш — единственный источник данных, и удалять его нельзя.
 */
@Singleton
class YaniContactTracker @Inject constructor() {
    private val contact = CompletableDeferred<Unit>()

    /** Был ли в этом запуске ответ yani. */
    val hasContact: Boolean get() = contact.isCompleted

    fun onResponse(statusCode: Int) {
        if (statusCode < SERVER_ERROR_FROM) contact.complete(Unit)
    }

    /** Ждёт первого ответа yani в этом запуске; не вернётся, если yani так и не ответил. */
    suspend fun awaitContact() = contact.await()

    private companion object {
        const val SERVER_ERROR_FROM = 500
    }
}
