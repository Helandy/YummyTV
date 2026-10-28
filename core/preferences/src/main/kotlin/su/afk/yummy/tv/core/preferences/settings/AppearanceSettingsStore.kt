package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.core.model.settings.LibrarySortDirection
import su.afk.yummy.tv.core.model.settings.LibraryTabKind
import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.TitleCardLayout
import su.afk.yummy.tv.core.model.settings.PosterQuality

/** Оформление интерфейса: тема, постеры, порядок и размеры карточек. */
interface AppearanceSettingsStore {

    val posterQuality: Flow<PosterQuality>
    val posterCardSize: Flow<PosterCardSize>
    val titleCardLayout: Flow<TitleCardLayout>
    val showCatalogTitleYear: Flow<Boolean>

    /** Показывать у серий пометки «филлер» и «рекап». */
    val showEpisodeFlags: Flow<Boolean>
    val showLibraryTitleYear: Flow<Boolean>
    val libraryContinueWatchingCardSize: Flow<LibraryContinueWatchingCardSize>
    val librarySort: Flow<LibrarySort>
    val librarySortDirection: Flow<LibrarySortDirection>
    val appTheme: Flow<AppTheme>
    val backgroundStyle: Flow<BackgroundStyle>

    /** Порядок вкладок на экране библиотеки. */
    val libraryTabOrder: Flow<List<LibraryTabKind>>

    /** Показывать ли праздничное оформление (гирлянда, тыквы) в сезон. */
    val seasonalDecorationsEnabled: Flow<Boolean>

    /** Показывать ли на главной блок новых серий из списков пользователя. */
    val newEpisodesSectionEnabled: Flow<Boolean>

    /** Списки, по которым блок новых серий отбирает тайтлы. */
    val newEpisodesSources: Flow<Set<NewEpisodesSource>>

    suspend fun setPosterQuality(quality: PosterQuality)

    /** Сохранено ли качество постеров явно (выбором пользователя или результатом пробы форматов). */
    suspend fun isPosterQualityStored(): Boolean

    /**
     * Записывает [quality] как качество постеров, только если пользователь его ещё не выбирал и
     * проба форматов не сохраняла результат. Выбор пользователя никогда не перезаписывается.
     *
     * @return `true`, если значение записано; `false`, если ключ уже был.
     */
    suspend fun initPosterQualityIfAbsent(quality: PosterQuality): Boolean

    /**
     * Для обновляющихся пользователей: если качество не сохранено, закрепляет то, что они фактически
     * имели до появления пробы форматов (Android 12+ — Mega, иначе Standard).
     */
    suspend fun pinLegacyPosterQualityIfAbsent(): Boolean
    suspend fun setPosterCardSize(size: PosterCardSize)
    suspend fun setTitleCardLayout(layout: TitleCardLayout)
    suspend fun setShowCatalogTitleYear(enabled: Boolean)
    suspend fun setShowEpisodeFlags(enabled: Boolean)
    suspend fun setShowLibraryTitleYear(enabled: Boolean)
    suspend fun setLibraryContinueWatchingCardSize(size: LibraryContinueWatchingCardSize)
    suspend fun setLibrarySort(sort: LibrarySort)
    suspend fun setLibrarySortDirection(direction: LibrarySortDirection)
    suspend fun setAppTheme(theme: AppTheme)
    suspend fun setBackgroundStyle(style: BackgroundStyle)
    suspend fun setLibraryTabOrder(order: List<LibraryTabKind>)
    suspend fun setNewEpisodesSectionEnabled(enabled: Boolean)
    suspend fun setSeasonalDecorationsEnabled(enabled: Boolean)

    /** Запоминает праздничный период, о котором пользователю уже показали диалог. */
    suspend fun setSeasonalAnnouncedKey(key: String)
    suspend fun setNewEpisodesSources(sources: Set<NewEpisodesSource>)

    companion object {
        val defaultLibraryTabOrder: List<LibraryTabKind> = LibraryTabKind.entries
    }
}
