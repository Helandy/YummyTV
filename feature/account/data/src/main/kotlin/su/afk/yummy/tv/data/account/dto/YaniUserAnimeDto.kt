package su.afk.yummy.tv.data.account.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniUserAnimeDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val poster: YaniAccountPosterDto? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val user: YaniAnimeUserDto? = null,
    val date: Long? = null,
    /**
     * Дата выхода следующей серии, epoch-секунды. В списках пользователя это плоское поле,
     * а не блок `episodes` с `next_date`, как в `/anime/{id}` и расписании. У завершённых
     * тайтлов здесь дата последней вышедшей серии либо поля нет вовсе.
     */
    @SerialName("next_episode") val nextEpisode: Long? = null,
    /**
     * Сезон выхода (квартал года): 1 — зима, 2 — весна, 3 — лето, 4 — осень, 0 — неизвестен
     * (проверено на живом ответе). Тип держим сырым, потому что у yani он местами плавает
     * между числом и слагом `winter`/`spring`/`summer`/`fall`, а несовпадение типа уронило бы
     * разбор всего списка целиком; разбираем вручную в `toAnimeSeason`.
     */
    val season: JsonElement? = null,
)
