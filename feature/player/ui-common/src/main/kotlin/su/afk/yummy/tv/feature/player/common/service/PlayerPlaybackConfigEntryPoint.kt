package su.afk.yummy.tv.feature.player.common.service

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerPlaybackConfigEntryPoint {
    fun playerPlaybackConfig(): PlayerPlaybackConfig
}

@Composable
fun rememberPlayerPlaybackConfig(): PlayerPlaybackConfig {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        EntryPointAccessors.fromApplication(context, PlayerPlaybackConfigEntryPoint::class.java)
            .playerPlaybackConfig()
    }
}
