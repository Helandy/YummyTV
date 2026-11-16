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
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore

/**
 * Отладочный приёмник для покадрового сравнения видеоэффектов. Регистрируется только в debuggable-сборках.
 *
 * - `--ez deblur true|false` — записывает настройку Deblur; плеер применяет её так же, как при переключении
 *   из меню (на лету или с перезапуском потока, см. [PlayerVideoDeblur]).
 * - `--el pos_ms N` — точная перемотка с паузой, чтобы снять кадр скриншотом.
 *
 * `adb shell am broadcast -a su.afk.yummy.tv.debug.PLAYER_FRAME --el pos_ms 800000`
 */
@OptIn(UnstableApi::class)
internal class PlayerDebugFrameReceiver(
    private val player: ExoPlayer,
    private val settingsStore: PlayerSettingsStore,
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.hasExtra(EXTRA_DEBLUR)) {
            val enabled = intent.getBooleanExtra(EXTRA_DEBLUR, false)
            runBlocking { settingsStore.setDeblurEnabled(enabled) }
        }
        if (intent.hasExtra(EXTRA_POSITION_MS)) {
            player.pause()
            player.seekTo(intent.getLongExtra(EXTRA_POSITION_MS, 0L))
        }
    }

    companion object {
        const val ACTION = "su.afk.yummy.tv.debug.PLAYER_FRAME"
        private const val EXTRA_DEBLUR = "deblur"
        private const val EXTRA_POSITION_MS = "pos_ms"

        /** Возвращает зарегистрированный приёмник или null, если сборка не debuggable. */
        fun registerIfDebuggable(
            context: Context,
            player: ExoPlayer,
            settingsStore: PlayerSettingsStore,
        ): PlayerDebugFrameReceiver? {
            val debuggable = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
            if (!debuggable) return null
            return PlayerDebugFrameReceiver(player, settingsStore).also {
                ContextCompat.registerReceiver(context, it, IntentFilter(ACTION), ContextCompat.RECEIVER_EXPORTED)
            }
        }
    }
}
