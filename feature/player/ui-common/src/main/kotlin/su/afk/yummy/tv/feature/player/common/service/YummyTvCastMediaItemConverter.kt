package su.afk.yummy.tv.feature.player.common.service

import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaQueueItem

/**
 * Часть балансеров (например ok.ru CDN) отдаёт прямые видео-URL без расширения в пути - только
 * query-параметры (`https://vd462.okcdn.ru/?expires=...`). [PlayerMediaItemFactory] в этом случае
 * оставляет MimeType не заданным: локальный ExoPlayer сам определяет контейнер по содержимому
 * ответа, так что для него это не проблема. Cast-ресивер так не умеет - без явного
 * `MediaInfo.contentType` он не может выбрать пайплайн воспроизведения и виснет в вечной
 * буферизации (подтверждено логом `adb logcat`: playbackState застревает в BUFFERING без ошибки).
 * Подставляем video/mp4 как разумный дефолт только для конвертации под Cast, не трогая
 * [PlayerMediaItemFactory]/локальное воспроизведение.
 *
 * Заголовки запроса здесь сознательно не передаются. Приложение работает со штатным
 * Cast-ресивером (`DEFAULT_MEDIA_RECEIVER_APPLICATION_ID`): он за медиа ходит собственным
 * браузерным стеком Chromecast и своих заголовков к запросам не добавляет (для этого нужен
 * собственный CAF-ресивер, а `User-Agent`, `Referer`, `Origin` из браузерного JS всё равно не
 * ставятся). Для okcdn это не помеха: он проверяет класс UA, и UA самого Chromecast (`CrKey`)
 * принимает — замер `curl` 07.10.2026 по синтетическим строкам, **на устройстве не проверено**.
 * Если на устройстве вернётся `400`, чинить нужно не ресивером, а прокси в LAN. Подробности — в
 * `docs/cvh-player.md`.
 */
@UnstableApi
class YummyTvCastMediaItemConverter : MediaItemConverter {
    private val delegate = DefaultMediaItemConverter()

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem =
        delegate.toMediaItem(mediaQueueItem)

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val hasMimeType = mediaItem.localConfiguration?.mimeType != null
        val itemForCast = if (hasMimeType) {
            mediaItem
        } else {
            mediaItem.buildUpon().setMimeType(MimeTypes.VIDEO_MP4).build()
        }
        return delegate.toMediaQueueItem(itemForCast)
    }
}
