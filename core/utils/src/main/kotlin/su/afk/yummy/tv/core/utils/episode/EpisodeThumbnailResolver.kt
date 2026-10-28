package su.afk.yummy.tv.core.utils.episode

/**
 * Единственное место, где решается, откуда брать превью серии. И Coil-фетчер ([EpisodeThumbnailFetcher]), и места,
 * которым нужна готовая ссылка (Watch Next, обложка плеера, загрузки), ходят сюда, поэтому источники и их порядок
 * одинаковы везде.
 */
interface EpisodeThumbnailResolver {

    /** Прямая ссылка на кадр серии или `null`, если ни один источник его не дал. */
    suspend fun resolveUrl(thumbnail: EpisodeThumbnail): String?
}
