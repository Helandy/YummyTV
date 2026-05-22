package su.afk.yummy.tv.core.designsystem.presenter.baseViewModel

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage

interface UiState
interface UiEvent
interface UiEffect

abstract class BaseViewModelNew<S : UiState, E : UiEvent, F : UiEffect>(
    protected val savedStateHandle: SavedStateHandle
) : CoroutineVieModel() {

    protected abstract fun createInitialState(): S

    private val _state by lazy { MutableStateFlow(createInitialState()) }
    val state: StateFlow<S> by lazy { _state.asStateFlow() }

    val currentState: S get() = _state.value

    protected fun setState(reducer: S.() -> S) {
        _state.update {
            val newState = it.reducer()
            saveToSavedState(newState)
            newState
        }
    }

    protected open fun saveToSavedState(state: S) {}

    private val _effect = MutableSharedFlow<F>()
    val effect: SharedFlow<F> = _effect.asSharedFlow()

    protected fun setEffect(effect: F) {
        viewModelScope.launch { _effect.emit(effect) }
    }

    fun setEvent(event: E) = onEvent(event)
    protected abstract fun onEvent(event: E)

    protected abstract val errorHandler: IErrorHandlerUseCase
    protected abstract val retryStorage: RetryStorage

    override fun onError(exception: Throwable) {
        val retryKey = "${this::class.java.simpleName}:${System.nanoTime()}"
        retryStorage.put(retryKey) { onRetry() }
        errorHandler.parse(exception, navigate = true, retryKey = retryKey)
    }

    protected open fun onRetry() {}
}
