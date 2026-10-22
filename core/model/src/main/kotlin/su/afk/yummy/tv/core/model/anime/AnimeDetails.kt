package su.afk.yummy.tv.core.model.anime

data class AnimeDetails(
    val id: Int,
    val animeUrl: String,
    val title: String,
    val description: String,
    val poster: AnimePoster?,
    val rating: AnimeRating,
    val genres: List<AnimeGenre>,
    val year: Int?,
    val ageRating: String?,
    val views: Int?,
    val status: String?,
    val type: String?,
    val episodes: AnimeEpisodes?,
    val otherTitles: List<String>,
    val creators: List<AnimePerson>,
    val studios: List<AnimeStudio>,
    val viewingOrder: List<AnimeViewingOrderItem>,
    val screenshots: List<AnimeScreenshot>,
    val reviewsCount: Int = 0,
    /** Все комментарии тайтла, включая ответы. */
    val commentsCount: Int = 0,
    /** Только корневые комментарии, без ответов. */
    val parentCommentsCount: Int = 0,
    val postsCount: Int = 0,
    /** Сколько раз тайтл добавлен в пользовательские списки. */
    val listsCount: Int = 0,
    val trailersCount: Int = 0,
    /** Число видео блогеров по тайтлу. */
    val bloggerVideosCount: Int = 0,
    /** id тайтла в MyAnimeList — ключ для API названий и описаний серий. */
    val malId: Int? = null,
)
