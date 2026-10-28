package su.afk.yummy.tv.core.preferences.settings.datastore

import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.appThemeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.backgroundStyleKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.libraryContinueWatchingCardSizeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.librarySortDirectionKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.librarySortKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.libraryTabOrderKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.newEpisodesSectionEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.seasonalAnnouncedKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.seasonalDecorationsEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.newEpisodesSourcesKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.posterCardSizeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.titleCardLayoutKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.posterQualityKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showLibraryTitleYearKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showCatalogTitleYearKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showEpisodeFlagsKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreAppearanceSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
) : AppearanceSettingsStore {

    override val posterQuality: Flow<PosterQuality> =
        store.enumFlow(posterQualityKey, defaultPosterQuality)

    override val posterCardSize: Flow<PosterCardSize> =
        store.enumFlow(posterCardSizeKey, PosterCardSize.STANDARD)
    override val titleCardLayout: Flow<TitleCardLayout> =
        store.enumFlow(titleCardLayoutKey, TitleCardLayout.TILE)

    override val showCatalogTitleYear: Flow<Boolean> = store.boolean(showCatalogTitleYearKey, false)
    override val showEpisodeFlags: Flow<Boolean> = store.boolean(showEpisodeFlagsKey, true)

    override val seasonalDecorationsEnabled: Flow<Boolean> =
        store.boolean(seasonalDecorationsEnabledKey, true)

    override val newEpisodesSectionEnabled: Flow<Boolean> =
        store.boolean(newEpisodesSectionEnabledKey, true)

    override val newEpisodesSources: Flow<Set<NewEpisodesSource>> =
        store.stringSet(newEpisodesSourcesKey).map { it.toNewEpisodesSources() }

    override val showLibraryTitleYear: Flow<Boolean> =
        store.boolean(showLibraryTitleYearKey, false)

    override val libraryContinueWatchingCardSize: Flow<LibraryContinueWatchingCardSize> =
        store.enumFlow(libraryContinueWatchingCardSizeKey, LibraryContinueWatchingCardSize.LARGE)

    override val librarySort: Flow<LibrarySort> =
        store.enumFlow(librarySortKey, LibrarySort.ADDED_DATE)

    override val librarySortDirection: Flow<LibrarySortDirection> =
        store.enumFlow(librarySortDirectionKey, LibrarySortDirection.DESC)

    override val appTheme: Flow<AppTheme> = store.enumFlow(appThemeKey, AppTheme.WARM_AMBER)

    override val backgroundStyle: Flow<BackgroundStyle> =
        store.enumFlow(backgroundStyleKey, BackgroundStyle.DARK)

    override val libraryTabOrder: Flow<List<LibraryTabKind>> =
        store.data.map { prefs -> prefs[libraryTabOrderKey].toLibraryTabOrder() }

    override suspend fun setPosterQuality(quality: PosterQuality) =
        store.setEnum(posterQualityKey, quality)

    override suspend fun isPosterQualityStored(): Boolean =
        posterQualityKey in store.data.first()

    override suspend fun pinLegacyPosterQualityIfAbsent(): Boolean =
        initPosterQualityIfAbsent(legacyPosterQuality(Build.VERSION.SDK_INT))

    override suspend fun initPosterQualityIfAbsent(quality: PosterQuality): Boolean {
        var written = false
        store.edit { prefs ->
            if (posterQualityKey !in prefs) {
                prefs[posterQualityKey] = quality.name
                written = true
            }
        }
        return written
    }

    override suspend fun setPosterCardSize(size: PosterCardSize) =
        store.setEnum(posterCardSizeKey, size)

    override suspend fun setTitleCardLayout(layout: TitleCardLayout) =
        store.setEnum(titleCardLayoutKey, layout)

    override suspend fun setSeasonalDecorationsEnabled(enabled: Boolean) =
        store.setBoolean(seasonalDecorationsEnabledKey, enabled)

    override suspend fun setSeasonalAnnouncedKey(key: String) {
        store.edit { prefs -> prefs[seasonalAnnouncedKey] = key }
    }

    override suspend fun setNewEpisodesSectionEnabled(enabled: Boolean) =
        store.setBoolean(newEpisodesSectionEnabledKey, enabled)

    override suspend fun setNewEpisodesSources(sources: Set<NewEpisodesSource>) {
        store.edit { prefs -> prefs[newEpisodesSourcesKey] = sources.toStoredNames() }
    }

    override suspend fun setShowCatalogTitleYear(enabled: Boolean) =
        store.setBoolean(showCatalogTitleYearKey, enabled)

    override suspend fun setShowEpisodeFlags(enabled: Boolean) =
        store.setBoolean(showEpisodeFlagsKey, enabled)

    override suspend fun setShowLibraryTitleYear(enabled: Boolean) =
        store.setBoolean(showLibraryTitleYearKey, enabled)

    override suspend fun setLibraryContinueWatchingCardSize(size: LibraryContinueWatchingCardSize) =
        store.setEnum(libraryContinueWatchingCardSizeKey, size)

    override suspend fun setLibrarySort(sort: LibrarySort) =
        store.setEnum(librarySortKey, sort)

    override suspend fun setLibrarySortDirection(direction: LibrarySortDirection) =
        store.setEnum(librarySortDirectionKey, direction)

    override suspend fun setAppTheme(theme: AppTheme) = store.setEnum(appThemeKey, theme)

    override suspend fun setBackgroundStyle(style: BackgroundStyle) =
        store.setEnum(backgroundStyleKey, style)

    override suspend fun setLibraryTabOrder(order: List<LibraryTabKind>) {
        store.edit { prefs ->
            prefs[libraryTabOrderKey] = order.normalizedLibraryTabOrder()
                .joinToString(LIBRARY_TAB_ORDER_SEPARATOR) { it.name }
        }
    }

    internal companion object {
        /**
         * Безопасный запасной вариант, пока качество не выбрано: на первичной настройке оно
         * определяется реальной пробой декодирования AVIF/WebP на устройстве.
         */
        val defaultPosterQuality: PosterQuality = PosterQuality.STANDARD
    }
}
