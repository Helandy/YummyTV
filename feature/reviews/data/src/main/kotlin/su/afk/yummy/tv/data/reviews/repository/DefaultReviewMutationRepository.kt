package su.afk.yummy.tv.data.reviews.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import su.afk.yummy.tv.domain.reviews.repository.ReviewMutationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultReviewMutationRepository @Inject constructor() : ReviewMutationRepository {
    private val mutableVersion = MutableStateFlow(0L)

    override val version: StateFlow<Long> = mutableVersion.asStateFlow()

    override fun notifyChanged() {
        mutableVersion.update { it + 1 }
    }
}
