package su.afk.yummy.tv.feature.home.handler

import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.anime.usecase.SetAnimeRecommendationIgnoredUseCase
import javax.inject.Inject

/** Серверная часть скрытия рекомендаций: проверка входа и сам запрос. */
internal class HomeRecommendationHandler @Inject constructor(
    private val settingsStore: SettingsStore,
    private val setAnimeRecommendationIgnored: SetAnimeRecommendationIgnoredUseCase,
) {
    suspend fun isAuthorized(): Boolean = settingsStore.yaniUserId.first() > 0

    /** `true`, если сервер принял изменение; ошибка запроса считается отказом. */
    suspend fun setIgnored(animeId: Int, hidden: Boolean): Boolean =
        runSuspendCatching { setAnimeRecommendationIgnored(animeId, hidden) }.getOrDefault(false)
}
