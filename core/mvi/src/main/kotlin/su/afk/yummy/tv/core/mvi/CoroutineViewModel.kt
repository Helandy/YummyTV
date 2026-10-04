package su.afk.yummy.tv.core.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.SupervisorJob

abstract class CoroutineViewModel : ViewModel() {

    private val handler = CoroutineExceptionHandler { _, exception ->
        onError(exception)
    }

    protected abstract fun onError(exception: Throwable)

    private val viewModelJob = SupervisorJob()
    protected val viewModelScope = CoroutineScope(Main + viewModelJob + handler)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}
