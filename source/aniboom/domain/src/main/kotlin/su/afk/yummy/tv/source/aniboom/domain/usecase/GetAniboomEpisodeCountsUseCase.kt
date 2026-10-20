package su.afk.yummy.tv.source.aniboom.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTitle
import su.afk.yummy.tv.source.aniboom.domain.repository.AniboomRepository
import javax.inject.Inject

/**
 * Считает, сколько серий реально доступно в каждой озвучке AniBoom. Сайт включает в счётчик и серию-заглушку, которая ещё не вышла,
 * поэтому при [checkLast] последняя серия каждой озвучки проверяется в эмбеде (параллельно): нет видео — у озвучки на серию меньше.
 * Сбой сети или таймаут проверки не прячут серию: считается, что она доступна. Без [checkLast] запросов нет.
 *
 * Возвращает озвучку (`translation`) → число серий.
 */
class GetAniboomEpisodeCountsUseCase @Inject constructor(
    private val repository: AniboomRepository,
) {
    suspend operator fun invoke(title: AniboomTitle, checkLast: Boolean): Map<Int, Int> {
        val last = title.episodesAvailable
        if (!checkLast || last <= 1) return title.dubbings.associate { it.translation to last }

        return coroutineScope {
            title.dubbings.map { dubbing ->
                async {
                    val target = AniboomTarget(title.aniboomId, last, dubbing.translation)
                    val exists = try {
                        withTimeoutOrNull(CHECK_TIMEOUT_MS) { repository.hasEpisode(target) } ?: true
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        true
                    }
                    dubbing.translation to if (exists) last else last - 1
                }
            }.awaitAll().toMap()
        }
    }

    private companion object {
        const val CHECK_TIMEOUT_MS = 4_000L
    }
}
