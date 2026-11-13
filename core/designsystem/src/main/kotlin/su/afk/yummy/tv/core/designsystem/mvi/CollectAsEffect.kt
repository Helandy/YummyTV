package su.afk.yummy.tv.core.designsystem.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow

/**
 * Собирает одноразовые эффекты экрана, пока он в композиции.
 *
 * Блок всегда вызывается с актуальными лямбдами (без перезапуска сбора), а подписка
 * пересоздаётся только при смене самого потока.
 */
@Composable
fun <E> Flow<E>.collectAsEffect(block: suspend (E) -> Unit) {
    val currentBlock by rememberUpdatedState(block)
    LaunchedEffect(this) {
        collect { currentBlock(it) }
    }
}
