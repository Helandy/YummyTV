package su.afk.yummy.tv.data.library.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Запись экспорта пользовательского списка Shikimori (`*_animes.json`). При импорте также
 * принимает формат ответа `/api/users/{id}/anime_rates` с вложенным объектом [anime].
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ShikimoriListEntryDto(
    @SerialName("target_title") val targetTitle: String? = null,
    @SerialName("target_title_ru") val targetTitleRu: String? = null,
    @SerialName("target_id") val targetId: Int? = null,
    @EncodeDefault
    @SerialName("target_type") val targetType: String? = TARGET_TYPE_ANIME,
    @EncodeDefault
    val score: Int = 0,
    val status: String? = null,
    @EncodeDefault
    val rewatches: Int = 0,
    @EncodeDefault
    val episodes: Int = 0,
    @EncodeDefault
    val text: String? = null,
    val anime: ShikimoriNestedAnimeDto? = null,
) {
    companion object {
        const val TARGET_TYPE_ANIME = "Anime"
    }
}

@Serializable
data class ShikimoriNestedAnimeDto(
    val id: Int? = null,
    val name: String? = null,
    val russian: String? = null,
    val kind: String? = null,
    val episodes: Int? = null,
)
