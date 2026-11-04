package su.afk.yummy.tv.core.model.anime

/**
 * Кадры тайтла из TMDB (приходят от YummyTV API вместе с названиями серий).
 *
 * [firstEpisodeStillUrls] — кадры серии 1, лучший первым; [episodePreviewUrls] — кадр каждой серии по ключу
 * `episodeGroupKey()` (`"01"` и `"1"` — одна серия). [episodeMirrorUrls] — своя копия кадра серии в AVIF на нашем
 * хосте по тому же ключу; файла там может ещё не быть (хост ответит 404). Всё пустое, если данных нет.
 */
data class AnimeTmdbImages(
    val firstEpisodeStillUrls: List<String> = emptyList(),
    val episodePreviewUrls: Map<String, String> = emptyMap(),
    val episodeMirrorUrls: Map<String, String> = emptyMap(),
)
