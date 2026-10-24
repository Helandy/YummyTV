package su.afk.yummy.tv.source.anilibria.domain.model

/**
 * Серия релиза AniLibria. Ссылки — HLS-плейлисты; отсутствующее качество = `null`. В списке серий с сервера YummyTV ссылок нет
 * вовсе: они приходят только из релиза, запрошенного при запуске серии.
 */
data class AniLibriaEpisode(
    val ordinal: Int,
    val name: String?,
    val hls480: String?,
    val hls720: String?,
    val hls1080: String?,
    val durationSec: Int?,
    val opening: AniLibriaSkip?,
)
