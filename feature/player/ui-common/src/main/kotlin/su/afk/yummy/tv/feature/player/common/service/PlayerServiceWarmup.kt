package su.afk.yummy.tv.feature.player.common.service

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

/**
 * Держит лишнее подключение к [PlayerMediaSessionService], пока экран плеера виден.
 *
 * Два эффекта. Сервис, ExoPlayer и кэш поднимаются сразу при входе на экран, одновременно с
 * извлечением ссылки, а не после него. И при переключении серии view плеера уходит из композиции
 * и шлёт STOP_SERVICE, но привязанный клиент не даёт системе уничтожить сервис: следующая серия
 * подключается к тому же экземпляру (ветка «Service reused after stop request» в onConnect), а не
 * собирает сервис и плеер заново. На ON_STOP подключение снимается, чтобы свёрнутое приложение не
 * держало плеер в памяти.
 */
@Composable
fun PlayerServiceWarmupEffect() {
    if (LocalInspectionMode.current) return
    val context = LocalContext.current
    LifecycleStartEffect(context) {
        // Только Application: см. rememberPlayerPlaybackSessionClient.
        val appContext = context.applicationContext
        val token =
            SessionToken(appContext, ComponentName(appContext, PlayerMediaSessionService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        onStopOrDispose { MediaController.releaseFuture(future) }
    }
}
