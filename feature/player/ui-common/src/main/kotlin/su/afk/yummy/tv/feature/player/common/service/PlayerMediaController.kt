package su.afk.yummy.tv.feature.player.common.service

import androidx.compose.runtime.Composable
import androidx.media3.session.MediaController

/** Сохраняет прежний mobile API: отключение контроллера не останавливает сервис автоматически. */
@Composable
fun rememberPlayerMediaController(): MediaController? =
    rememberPlayerPlaybackSessionClient().player
