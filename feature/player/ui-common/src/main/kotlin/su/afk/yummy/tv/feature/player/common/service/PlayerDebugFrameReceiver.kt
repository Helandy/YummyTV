package su.afk.yummy.tv.feature.player.common.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.runBlocking
import su.afk.yummy.tv.core.model.settings.VideoEnhancementMode
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore

/**
 * Отладочный приёмник для покадрового сравнения видеоэффектов. Регистрируется только в debuggable-сборках.
 *
 * - `--es enhancement OFF|SHARPEN|QUALITY` — записывает режим улучшения видео; плеер применяет его так же, как
 *   при выборе в меню (включение перезапускает поток, выключение пересобирает плеер, см. [PlayerVideoEnhancement]).
 * - `--ez deblur true|false` — то же для SHARPEN/OFF.
 * - `--el pos_ms N` — точная перемотка с паузой, чтобы снять кадр скриншотом.
 *
 * `adb shell am broadcast -a su.afk.yummy.tv.debug.PLAYER_FRAME --el pos_ms 800000`
 */
@OptIn(UnstableApi::class)
internal class PlayerDebugFrameReceiver(
    private val playerProvider: () -> ExoPlayer?,
    private val settingsStore: PlayerSettingsStore,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mode = when {
            intent.hasExtra(EXTRA_ENHANCEMENT) ->
                VideoEnhancementMode.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_ENHANCEMENT) }
            intent.hasExtra(EXTRA_DEBLUR) ->
                if (intent.getBooleanExtra(EXTRA_DEBLUR, false)) VideoEnhancementMode.SHARPEN else VideoEnhancementMode.OFF
            else -> null
        }
        if (mode != null) runBlocking { settingsStore.setVideoEnhancementMode(mode) }
        val player = playerProvider() ?: return
        if (intent.hasExtra(EXTRA_POSITION_MS)) {
            player.pause()
            player.seekTo(intent.getLongExtra(EXTRA_POSITION_MS, 0L))
        }
    }

    companion object {
        const val ACTION = "su.afk.yummy.tv.debug.PLAYER_FRAME"
        private const val EXTRA_ENHANCEMENT = "enhancement"
        private const val EXTRA_DEBLUR = "deblur"
        private const val EXTRA_POSITION_MS = "pos_ms"

        /** Возвращает зарегистрированный приёмник или null, если сборка не debuggable. */
        fun registerIfDebuggable(
            context: Context,
            playerProvider: () -> ExoPlayer?,
            settingsStore: PlayerSettingsStore,
        ): PlayerDebugFrameReceiver? {
            val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            if (!debuggable) return null
            return PlayerDebugFrameReceiver(playerProvider, settingsStore).also {
                ContextCompat.registerReceiver(context, it, IntentFilter(ACTION), ContextCompat.RECEIVER_EXPORTED)
            }
        }
    }
}
