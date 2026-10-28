package su.afk.yummy.tv.core.model.anime

/** Источник готовых ссылок на превью серий (Tenrai); запасной источник — Kodik. */
interface EpisodePreviewProvider {

    /** Ссылка на кадр серии [episode] тайтла [animeId] или `null`, если серия не опознана, кадра нет или он не загрузился. */
    suspend fun previewUrl(animeId: Int, episode: String): String?
}
