package su.afk.yummy.tv.core.designsystem.model

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus
import su.afk.yummy.tv.core.model.anime.CardListStatus

/**
 * Тайтл из пользовательского списка — общая модель карточек библиотеки и списков профиля
 * на мобилке и ТВ. Пустые поля карточка просто не рисует.
 */
@Immutable
data class TitleListCardUi(
    val animeId: Int,
    val title: String,
    val posterUrl: String?,
    /** Оценка сайта. */
    val siteRating: Double? = null,
    /** Оценка пользователя, 1..10. */
    val userRating: Int? = null,
    val year: Int? = null,
    val releaseStatus: AnimeReleaseStatus? = null,
    /** Список пользователя; задаётся, только когда он не очевиден из текущей вкладки. */
    val listStatus: CardListStatus? = null,
    /** Пометка «в любимых»; задаётся, только когда это не вкладка «Любимые». */
    val isFavorite: Boolean = false,
    /** Готовая дата добавления / обновления. */
    val dateText: String? = null,
    /** Дата выхода следующей серии, epoch-секунды. */
    val nextEpisodeAtSeconds: Long? = null,
)
