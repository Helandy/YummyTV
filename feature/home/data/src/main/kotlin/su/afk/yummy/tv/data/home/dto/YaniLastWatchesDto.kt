package su.afk.yummy.tv.data.home.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YaniLastWatchesDto(val response: List<YaniLastWatchDto> = emptyList())

/** Последняя просмотренная серия тайтла: без видео и озвучки — только номер серии и позиция. */
@Serializable
data class YaniLastWatchDto(
    @SerialName("anime_id") val animeId: Int = 0,
    val title: String = "",
    val description: String = "",
    val poster: YaniPosterDto? = null,
    @SerialName("ep_title") val episode: String = "",
    /** Прогресс пользователя в серии: где он остановился, секунды от начала. */
    @SerialName("end_time") val endTime: Int = 0,
    /** Длина серии в секундах. */
    val duration: Int = 0,
    /** Unix-время просмотра в секундах. */
    val date: Long = 0L,
    val screenshot: YaniLastWatchScreenshotDto? = null,
)

@Serializable
data class YaniLastWatchScreenshotDto(
    val sizes: YaniLastWatchScreenshotSizesDto? = null,
)

@Serializable
data class YaniLastWatchScreenshotSizesDto(
    val small: String? = null,
    val full: String? = null,
)
