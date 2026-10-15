package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.repository.RemoteContinueWatchingRepository
import javax.inject.Inject

/** Запрашивает обновление серверной истории просмотра для «Продолжить просмотр» на главной. */
class RefreshRemoteContinueWatchingUseCase @Inject constructor(
    private val remoteRepository: RemoteContinueWatchingRepository,
) {
    operator fun invoke() = remoteRepository.requestRefresh()
}
