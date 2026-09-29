package su.afk.yummy.tv.feature.player.common.service

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.RemoteCastPlayer
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.utils.cast.CastSupport
import su.afk.yummy.tv.feature.player.common.utils.PLAYER_SERVICE_LOG_TAG
import javax.inject.Inject

/** Оборачивает локальный ExoPlayer в CastPlayer там, где Cast поддерживается. */
@OptIn(UnstableApi::class)
internal class PlayerCastPlayerFactory @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) {
    /**
     * Гейт на TV и на старые GMS - внутри CastSupport: RemoteCastPlayer.Builder сам зовёт
     * Cast.ensureInitialized(), то есть это второй вход в тот же фоновый путь, что и в
     * YummyTvApplication.setupCast().
     *
     * CastContext доступен только при наличии Google Play Services на устройстве - без них
     * CastContext.getSharedInstance() кидает исключение, поэтому локальный ExoPlayer остаётся
     * фолбэком, а не жёстким требованием.
     */
    fun createOrNull(context: Context, exoPlayer: ExoPlayer): CastPlayer? {
        if (!CastSupport.isSupported(context)) return null
        return try {
            CastPlayer.Builder(context)
                .setLocalPlayer(exoPlayer)
                .setRemotePlayer(
                    RemoteCastPlayer.Builder(context)
                        .setMediaItemConverter(YummyTvCastMediaItemConverter())
                        .build(),
                )
                .build()
        } catch (e: Exception) {
            analyticsTracker.log(PLAYER_SERVICE_LOG_TAG, e) { "CastPlayer unavailable" }
            null
        }
    }
}
