package su.afk.yummy.tv.core.mvi

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.isNetworkError

/**
 * База MVI-экрана: держит [state], раздаёт одноразовые [effect] и сводит обработку ошибок
 * к [ErrorHandler] — наследнику остаётся описать начальное состояние и реакцию на события.
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect> : CoroutineViewModel() {

    protected abstract fun createInitialState(): S

    private val _state by lazy { MutableStateFlow(createInitialState()) }
    val state: StateFlow<S> by lazy { _state.asStateFlow() }

    val currentState: S get() = _state.value

    protected fun setState(reducer: S.() -> S) {
        _state.update { it.reducer() }
    }

    private val _effect = MutableSharedFlow<F>()
    val effect: SharedFlow<F> = _effect.asSharedFlow()

    protected fun setEffect(effect: F) {
        viewModelScope.launch { _effect.emit(effect) }
    }

    fun setEvent(event: E) = onEvent(event)
    protected abstract fun onEvent(event: E)

    protected abstract val errorHandler: ErrorHandler
    protected abstract val retryStorage: RetryStorage

    override fun onError(exception: Throwable) {
        val retryKey = "${this::class.java.simpleName}:${System.nanoTime()}"
        retryStorage.put(retryKey) { onRetry() }
        errorHandler.parse(
            t = exception,
            navigate = true,
            retryKey = retryKey,
            owner = this::class.java.simpleName,
        )
    }

    protected open fun onRetry() {}

    /**
     * Текст ошибки для inline-состояния экрана. Сетевые сбои — локализованным сообщением из
     * [ErrorHandler], а исходный текст (`Unable to resolve host ...`) второй строкой;
     * остальное — как раньше: текст исключения, иначе [fallback].
     */
    protected fun Throwable.userMessage(fallback: String? = null): String =
        if (isNetworkError()) {
            val text = errorHandler.parse(this).message
            message?.takeIf { it.isNotBlank() }?.let { "$text\n$it" } ?: text
        } else {
            message ?: fallback ?: errorHandler.parse(this).message
        }
}
