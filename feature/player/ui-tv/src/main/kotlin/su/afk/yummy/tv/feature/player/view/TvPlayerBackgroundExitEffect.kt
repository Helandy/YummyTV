package su.afk.yummy.tv.feature.player.view

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/** Убирает TV-плеер из навигационного стека, когда приложение полностью уходит в фон. */
@Composable
internal fun TvPlayerBackgroundExitEffect(onBackgrounded: () -> Unit) {
    val currentOnBackgrounded by rememberUpdatedState(onBackgrounded)
    val context = LocalContext.current
    val lifecycleOwner = remember(context) {
        context.findActivity() as? LifecycleOwner ?: ProcessLifecycleOwner.get()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) currentOnBackgrounded()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
