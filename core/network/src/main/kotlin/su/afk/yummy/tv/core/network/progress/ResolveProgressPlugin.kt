package su.afk.yummy.tv.core.network.progress

import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.ResolveDetail
import su.afk.yummy.tv.core.common.player.ResolveProgress

/**
 * Сообщает ход получения потока по HTTP-запросам: запрос ушёл ([PlayerStreamResolveStatus.FetchingPage]),
 * источник ответил ([PlayerStreamResolveStatus.ParsingPage]) или ответил ошибкой
 * ([PlayerStreamResolveStatus.ResponseError]). Для каждого запроса также отдаёт [ResolveDetail] с хостом,
 * кодом ответа или именем исключения и временем. Без [ResolveProgress] в контексте корутины ничего не делает.
 */
val ResolveProgressPlugin = createClientPlugin("ResolveProgress") {
    on(Send) { request ->
        val progress = currentCoroutineContext()[ResolveProgress] ?: return@on proceed(request)
        val host = request.url.host
        val startedAt = System.nanoTime()
        fun elapsedMs() = (System.nanoTime() - startedAt) / NANOS_IN_MS
        if (progress.emitsSteps) progress.report(PlayerStreamResolveStatus.FetchingPage)
        try {
            val call = proceed(request)
            val code = call.response.status
            if (progress.emitsSteps) {
                progress.report(
                    if (code.isSuccess()) PlayerStreamResolveStatus.ParsingPage else PlayerStreamResolveStatus.ResponseError,
                )
            }
            progress.reportDetail(ResolveDetail(host, code.value, error = null, elapsedMs = elapsedMs()))
            call
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            progress.reportDetail(
                ResolveDetail(host, httpCode = null, error = error::class.java.simpleName, elapsedMs = elapsedMs()),
            )
            throw error
        }
    }
}

private const val NANOS_IN_MS = 1_000_000L
