package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.utils.notifyBooleanMutationFailure
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository
import javax.inject.Inject

/**
 * Переключает подписку на новые серии выбранной озвучки.
 *
 * Сервер принимает `video_id` любой серии нужной озвучки и сам сводит его к тройке
 * «тайтл + балансер + озвучка».
 */
class SetVideoSubscriptionUseCase @Inject constructor(
    private val repository: VideoSubscriptionRepository,
    private val mutationErrorNotifier: AccountMutationErrorRepository,
) {
    suspend operator fun invoke(videoId: Int, subscribed: Boolean): Boolean =
        notifyBooleanMutationFailure(
            mutationErrorNotifier,
            if (subscribed) {
                AccountMutationAction.SET_VIDEO_SUBSCRIPTION
            } else {
                AccountMutationAction.REMOVE_VIDEO_SUBSCRIPTION
            },
        ) {
            repository.setSubscribed(videoId, subscribed)
        }
}
