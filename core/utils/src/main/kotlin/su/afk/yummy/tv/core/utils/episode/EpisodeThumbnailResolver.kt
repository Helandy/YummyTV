package su.afk.yummy.tv.core.utils.episode

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

/**
 * Единственное место, где решается, откуда брать превью серии. И Coil-фетчер ([EpisodeThumbnailFetcher]), и места,
 * которым нужна готовая ссылка (Watch Next, обложка плеера, загрузки), ходят сюда, поэтому источники и их порядок
 * одинаковы везде.
 */
interface EpisodeThumbnailResolver {

    /**
     * Ссылки на кадр серии по приоритету. Поток ленивый: следующая ссылка (например, сетевой резолв Kodik)
     * вычисляется, только когда предыдущую не удалось скачать, поэтому загрузчик пробует их по очереди.
     */
    fun candidateUrls(thumbnail: EpisodeThumbnail): Flow<String>

    /** Первая ссылка на кадр серии или `null`, если ни один источник его не дал. */
    suspend fun resolveUrl(thumbnail: EpisodeThumbnail): String? = candidateUrls(thumbnail).firstOrNull()
}
