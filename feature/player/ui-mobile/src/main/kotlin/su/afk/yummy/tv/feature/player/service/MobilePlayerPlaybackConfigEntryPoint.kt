package su.afk.yummy.tv.feature.player.service

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface MobilePlayerPlaybackConfigEntryPoint {
    fun mobilePlayerPlaybackConfig(): MobilePlayerPlaybackConfig
}

@Composable
internal fun rememberMobilePlayerPlaybackConfig(): MobilePlayerPlaybackConfig {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        EntryPointAccessors.fromApplication(
            context,
            MobilePlayerPlaybackConfigEntryPoint::class.java,
        ).mobilePlayerPlaybackConfig()
    }
}
