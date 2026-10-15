package su.afk.yummy.tv.source.anime4k.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kApiException
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreamsResult
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget
import su.afk.yummy.tv.source.anime4k.domain.repository.Anime4kRepository
import su.afk.yummy.tv.source.anime4k.domain.repository.RewardedAdRepository
import javax.inject.Inject

/**
 * Открывает 4K-озвучку: спрашивает согласие на рекламу, берёт тикет, показывает рекламу, при необходимости дожидается минимального времени
 * с момента выдачи тикета и обменивает его на ссылки. Без полученной награды ссылок не будет.
 */
class ResolveAnime4kStreamsUseCase @Inject constructor(
    private val repository: Anime4kRepository,
    private val ads: RewardedAdRepository,
) {
    suspend operator fun invoke(target: Anime4kTarget): Anime4kStreamsResult =
        try {
            // Согласие спрашиваем первым: пока человек читает вопрос, тикет не должен тикать.
            if (!ads.askConsent()) return Anime4kStreamsResult.Declined

            // Тикет берётся ДО рекламы: сервер требует паузу от выдачи тикета, и её покрывает показ рекламы.
            val ticket = repository.requestTicket()
            val issuedAtMs = System.currentTimeMillis()

            if (!ads.showRewardedAd()) {
                Anime4kStreamsResult.AdNotRewarded
            } else {
                val remainingMs = ticket.notBeforeSec * MS_IN_SECOND - (System.currentTimeMillis() - issuedAtMs)
                if (remainingMs > 0) delay(remainingMs)
                Anime4kStreamsResult.Success(repository.getStreams(ticket, target))
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Anime4kApiException.RateLimited) {
            Anime4kStreamsResult.RateLimited
        } catch (_: Anime4kApiException.Rejected) {
            Anime4kStreamsResult.Rejected
        } catch (_: Anime4kApiException.NotFound) {
            Anime4kStreamsResult.NotFound
        } catch (error: Exception) {
            Anime4kStreamsResult.Failed(error.message)
        }

    private companion object {
        const val MS_IN_SECOND = 1000L
    }
}
