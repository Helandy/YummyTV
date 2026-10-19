package su.afk.yummy.tv.feature.player.common

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Мост между ExoPlayer в сервисе и UI: аналитические сигналы, которых нет у `MediaController`
 * (пропущенные кадры, оценка пропускной способности), для режима «Адаптивное» качество.
 * Сервис и UI живут в одном процессе, поэтому достаточно синглтона.
 */
@Singleton
class PlayerQualitySignals @Inject constructor() {
    private val mutableEvents = MutableSharedFlow<Signal>(
        extraBufferCapacity = BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<Signal> = mutableEvents.asSharedFlow()

    fun emitDroppedFrames(count: Int) {
        mutableEvents.tryEmit(Signal.DroppedFrames(count))
    }

    fun emitBandwidth(bitsPerSecond: Long) {
        mutableEvents.tryEmit(Signal.Bandwidth(bitsPerSecond))
    }

    sealed interface Signal {
        /** Декодер пропустил [count] кадров с прошлого отчёта. */
        data class DroppedFrames(val count: Int) : Signal

        /** Оценка пропускной способности ExoPlayer, бит/с. */
        data class Bandwidth(val bitsPerSecond: Long) : Signal
    }

    private companion object {
        const val BUFFER_CAPACITY = 64
    }
}
