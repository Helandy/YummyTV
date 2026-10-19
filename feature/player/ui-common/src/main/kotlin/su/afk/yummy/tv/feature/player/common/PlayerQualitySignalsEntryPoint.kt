package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerQualitySignalsEntryPoint {
    fun playerQualitySignals(): PlayerQualitySignals
}

/** Тот же синглтон [PlayerQualitySignals], в который пишет ExoPlayer сервиса. */
@Composable
fun rememberPlayerQualitySignals(): PlayerQualitySignals {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        EntryPointAccessors.fromApplication(context, PlayerQualitySignalsEntryPoint::class.java)
            .playerQualitySignals()
    }
}
