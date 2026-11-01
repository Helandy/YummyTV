package su.afk.yummy.tv.core.model.error

import io.ktor.client.plugins.ResponseException
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 * Сетевые/оффлайн-ошибки (нет DNS, нет соединения, таймаут) — не баги приложения. Все обрывы
 * соединения (UnknownHostException, ConnectException, SocketTimeoutException и т.п.) — наследники
 * [IOException], поэтому единая проверка здесь используется и для решения "не репортить в
 * аналитику", и для решения "поставить мутацию в offline-очередь, а не откатывать".
 */
fun Throwable.isNetworkError(): Boolean = this is IOException

/**
 * Сбой, после которого мутацию стоит оставить в offline-очереди, а не откатывать: связи нет
 * ([isNetworkError]), сервер или прокси ответили 5xx/408/425/429 либо вернули не JSON (HTML
 * captive portal или страница шлюза даёт [SerializationException]). Постоянный отказ (прочие 4xx)
 * сюда не входит: повтор ничего не изменит. Дальше повторы ограничивает воркер очереди.
 */
fun Throwable.isRetryableMutationFailure(): Boolean {
    if (isNetworkError() || this is SerializationException) return true
    val status = (this as? ResponseException)?.response?.status?.value ?: return false
    return status >= 500 || status == 408 || status == 425 || status == 429
}
