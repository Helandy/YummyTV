package su.afk.yummy.tv.data.messages.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import su.afk.yummy.tv.domain.messages.repository.MessagesMutationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultMessagesMutationRepository @Inject constructor() : MessagesMutationRepository {
    private val mutableVersion = MutableStateFlow(0L)

    override val version: StateFlow<Long> = mutableVersion.asStateFlow()

    override fun notifyChanged() {
        mutableVersion.update { it + 1 }
    }
}
