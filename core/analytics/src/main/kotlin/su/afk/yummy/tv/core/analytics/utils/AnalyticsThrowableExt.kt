package su.afk.yummy.tv.core.analytics.utils

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Короткое имя класса исключения для аналитики (или "unknown", если недоступно). */
fun Throwable.analyticsType(): String =
    this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

/**
 * Стоит ли репортить исключение в аналитику ошибок.
 *
 * Отмена корутины ([CancellationException], в т.ч. обфусцированный `JobCancellationException`
 * «Job was cancelled») и сетевые/оффлайн-ошибки ([IOException]: нет DNS, обрыв соединения,
 * таймаут) — не баги приложения, а внешние обстоятельства, поэтому шумят в логах впустую.
 * Ср. политику в `ErrorHandlerImpl`.
 */
fun Throwable.isReportableError(): Boolean =
    this !is CancellationException && this !is IOException

/**
 * HTTP 404 от API — штатный ответ («аниме удалено/нет такого»), а не сбой приложения.
 * Его шлют отдельным событием, а не error-логом.
 */
fun Throwable.isHttpNotFound(): Boolean =
    this is ResponseException && response.status == HttpStatusCode.NotFound
