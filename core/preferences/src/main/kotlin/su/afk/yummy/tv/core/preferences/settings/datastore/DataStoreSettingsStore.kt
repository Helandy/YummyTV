package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.NavTransitionStyle
import su.afk.yummy.tv.core.model.settings.BrowserUserAgentProfile
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.MainSettingsSnapshot
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.TitleCardLayout
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.SettingsSnapshot
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.AppLifecycleSettingsStore
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.core.preferences.settings.CacheSettingsStore
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SearchSettingsStore
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.advancedPlayerVolumeEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.appThemeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.askDubbingOnWatchKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.autoPlayNextEpisodeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.autoSkipDelaySecondsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.autoSkipOpeningsEndingsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.backgroundStyleKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.navTransitionStyleKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.browserUserAgentProfileKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.libraryContinueWatchingCardSizeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.libraryTabOrderKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.newEpisodesSectionEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.seasonalAnnouncedKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.seasonalDecorationsEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.newEpisodesSourcesKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.nextEpisodeSwitchDelaySecondsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.pictureInPictureEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.playerBufferProfileKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.playerControlsAutoHideSecondsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.playerOrientationModeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.posterCardSizeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.titleCardLayoutKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.posterQualityKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.adaptiveVideoQualityEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.preferredVideoQualityKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.previewCacheSizeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showLibraryTitleYearKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showOpeningOnTimelineKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showCatalogTitleYearKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showEpisodeFlagsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.autoManageLibraryListsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.suggestNextEpisodeOnWatchedKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.tvPlayerVolumeKeysEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.videoDownloadWifiOnlyKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.videoExportAutoEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.volumeStabilizationEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.watchNextEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniAvatarUrlKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniContentLanguageKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniNicknameKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniUnreadNotificationsCountKey
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.VideoDownloadSettingsStore
import su.afk.yummy.tv.core.preferences.settings.VideoExportSettingsStore
import su.afk.yummy.tv.core.preferences.settings.datastore.DataStoreAppearanceSettingsStore.Companion.defaultPosterQuality
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фасад над доменными хранилищами: делегирует им все поля и добавляет только агрегаты
 * ([settingsSnapshot], [mainSettingsSnapshot]). Снапшоты читают ключи одним проходом по
 * [SettingsDataStore.data], а не комбинируют десятки Flow — источник всё равно один.
 */
@Singleton
internal class DataStoreSettingsStore @Inject constructor(
    private val store: SettingsDataStore,
    appearance: AppearanceSettingsStore,
    player: PlayerSettingsStore,
    yaniAccount: YaniAccountSettingsStore,
    cache: CacheSettingsStore,
    videoExport: VideoExportSettingsStore,
    videoDownload: VideoDownloadSettingsStore,
    appLifecycle: AppLifecycleSettingsStore,
    search: SearchSettingsStore,
) : SettingsStore,
    AppearanceSettingsStore by appearance,
    PlayerSettingsStore by player,
    YaniAccountSettingsStore by yaniAccount,
    CacheSettingsStore by cache,
    VideoExportSettingsStore by videoExport,
    VideoDownloadSettingsStore by videoDownload,
    AppLifecycleSettingsStore by appLifecycle,
    SearchSettingsStore by search {

    override val settingsSnapshot: Flow<SettingsSnapshot> = store.data.map { prefs ->
        SettingsSnapshot(
            appTheme = prefs.enum(appThemeKey, AppTheme.WARM_AMBER),
            backgroundStyle = prefs.enum(backgroundStyleKey, BackgroundStyle.DARK),
            navTransitionStyle = prefs.enum(navTransitionStyleKey, NavTransitionStyle.FADE_SCALE),
            posterQuality = prefs.enum(posterQualityKey, defaultPosterQuality),
            posterCardSize = prefs.enum(posterCardSizeKey, PosterCardSize.STANDARD),
            titleCardLayout = prefs.enum(titleCardLayoutKey, TitleCardLayout.TILE),
            showCatalogTitleYear = prefs[showCatalogTitleYearKey] ?: false,
            showEpisodeFlags = prefs[showEpisodeFlagsKey] ?: true,
            newEpisodesSectionEnabled = prefs[newEpisodesSectionEnabledKey] ?: true,
            newEpisodesSources = prefs[newEpisodesSourcesKey].toNewEpisodesSources(),
            showLibraryTitleYear = prefs[showLibraryTitleYearKey] ?: false,
            libraryContinueWatchingCardSize = prefs.enum(
                libraryContinueWatchingCardSizeKey,
                LibraryContinueWatchingCardSize.LARGE,
            ),
            preferredVideoQuality = prefs.enum(
                preferredVideoQualityKey,
                PreferredVideoQuality.BEST,
            ),
            adaptiveVideoQualityEnabled = prefs[adaptiveVideoQualityEnabledKey] ?: true,
            watchNextEnabled = prefs[watchNextEnabledKey] ?: true,
            previewCacheSize = (prefs[previewCacheSizeKey] ?: 100).coerceIn(50, 500),
            autoSkipOpeningsEndings = prefs[autoSkipOpeningsEndingsKey] ?: false,
            autoSkipDelaySeconds = (prefs[autoSkipDelaySecondsKey] ?: 5).coerceIn(1, 15),
            showOpeningOnTimeline = prefs[showOpeningOnTimelineKey] ?: false,
            autoPlayNextEpisode = prefs[autoPlayNextEpisodeKey] ?: false,
            nextEpisodeSwitchDelaySeconds =
                (prefs[nextEpisodeSwitchDelaySecondsKey] ?: 10).coerceIn(0, 30),
            playerControlsAutoHideSeconds =
                (prefs[playerControlsAutoHideSecondsKey] ?: 4).coerceIn(1, 30000),
            askDubbingOnWatch = prefs[askDubbingOnWatchKey] ?: true,
            pictureInPictureEnabled = prefs[pictureInPictureEnabledKey] ?: true,
            playerOrientationMode = prefs.enum(
                playerOrientationModeKey,
                PlayerOrientationMode.SYSTEM,
            ),
            suggestNextEpisodeOnWatched = prefs[suggestNextEpisodeOnWatchedKey] ?: true,
            autoManageLibraryLists = prefs[autoManageLibraryListsKey] ?: true,
            watchedThresholds = prefs.watchedThresholds(),
            tvPlayerVolumeKeysEnabled = prefs[tvPlayerVolumeKeysEnabledKey] ?: false,
            advancedPlayerVolumeEnabled = prefs[advancedPlayerVolumeEnabledKey] ?: false,
            volumeStabilizationEnabled = prefs[volumeStabilizationEnabledKey] ?: false,
            playerBufferProfile = prefs.enum(playerBufferProfileKey, PlayerBufferProfile.SMALL),
            browserUserAgentProfile = prefs.enum(
                browserUserAgentProfileKey,
                BrowserUserAgentProfile.DEFAULT,
            ),
            videoExportAutoEnabled = prefs[videoExportAutoEnabledKey] ?: false,
            videoDownloadWifiOnly = prefs[videoDownloadWifiOnlyKey] ?: false,
            yaniApplicationToken = prefs.yaniApplicationToken(),
            contentLanguage = YaniContentLanguage.fromPreferenceValue(prefs[yaniContentLanguageKey])
                ?: store.resolveSystemContentLanguage(),
            libraryTabOrder = prefs[libraryTabOrderKey].toLibraryTabOrder(),
            seasonalDecorationsEnabled = prefs[seasonalDecorationsEnabledKey] ?: true,
        )
    }

    override val mainSettingsSnapshot: Flow<MainSettingsSnapshot> = store.data.map { prefs ->
        MainSettingsSnapshot(
            appTheme = prefs.enum(appThemeKey, AppTheme.WARM_AMBER),
            backgroundStyle = prefs.enum(backgroundStyleKey, BackgroundStyle.DARK),
            navTransitionStyle = prefs.enum(navTransitionStyleKey, NavTransitionStyle.FADE_SCALE),
            posterQuality = prefs.enum(posterQualityKey, defaultPosterQuality),
            posterCardSize = prefs.enum(posterCardSizeKey, PosterCardSize.STANDARD),
            yaniNickname = prefs[yaniNicknameKey].orEmpty(),
            yaniAvatarUrl = prefs[yaniAvatarUrlKey].orEmpty(),
            yaniUnreadNotificationsCount = prefs[yaniUnreadNotificationsCountKey] ?: 0,
            seasonalDecorationsEnabled = prefs[seasonalDecorationsEnabledKey] ?: true,
            seasonalAnnouncedKey = prefs[seasonalAnnouncedKey].orEmpty(),
        )
    }
}
