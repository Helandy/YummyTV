package su.afk.yummy.tv.domain.collection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionMutationNotifier @Inject constructor() {
    private val mutableVersion = MutableStateFlow(0L)

    val version = mutableVersion.asStateFlow()

    fun notifyChanged() {
        mutableVersion.update { it + 1 }
    }
}
