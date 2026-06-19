package su.afk.yummy.tv.domain.account.usecase

import su.afk.yummy.tv.domain.account.mutation.AccountMutationAction
import su.afk.yummy.tv.domain.account.mutation.AccountMutationErrorNotifier
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository
import javax.inject.Inject

/** Переключает подписку на обновления выбранного видео. */
class SetVideoSubscriptionUseCase @Inject constructor(
    private val repository: VideoSubscriptionRepository,
    private val mutationErrorNotifier: AccountMutationErrorNotifier,
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
