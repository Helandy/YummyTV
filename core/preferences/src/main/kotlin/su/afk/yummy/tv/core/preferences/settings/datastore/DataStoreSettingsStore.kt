package su.afk.yummy.tv.core.preferences.settings.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.MainSettingsSnapshot
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.SettingsSnapshot
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
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
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.detailsButtonOrderKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.libraryContinueWatchingCardSizeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.nextEpisodeSwitchDelaySecondsKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.pictureInPictureEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.playerBufferProfileKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.playerOrientationModeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.posterCardSizeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.posterQualityKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.preferredPlayerKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.preferredVideoQualityKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.previewCacheSizeKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.refreshContinueWatchingProgressOnLaunchKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showLibraryTitleYearKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showOpeningOnTimelineKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.showTopTitleYearKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.suggestNextEpisodeOnWatchedKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.tvPlayerVolumeKeysEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.videoExportAutoEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.volumeStabilizationEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.watchNextEnabledKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniAvatarUrlKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniContentLanguageKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniNicknameKey
import su.afk.yummy.tv.core.preferences.settings.SettingsPreferenceKeys.yaniUnreadNotificationsCountKey
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.VideoExportSettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
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
    appLifecycle: AppLifecycleSettingsStore,
    search: SearchSettingsStore,
) : SettingsStore,
    AppearanceSettingsStore by appearance,
    PlayerSettingsStore by player,
    YaniAccountSettingsStore by yaniAccount,
    CacheSettingsStore by cache,
    VideoExportSettingsStore by videoExport,
    AppLifecycleSettingsStore by appLifecycle,
    SearchSettingsStore by search {

    override val settingsSnapshot: Flow<SettingsSnapshot> = store.data.map { prefs ->
        SettingsSnapshot(
            appTheme = prefs.enum(appThemeKey, AppTheme.WARM_AMBER),
            backgroundStyle = prefs.enum(backgroundStyleKey, BackgroundStyle.DARK),
            posterQuality = prefs.enum(posterQualityKey, defaultPosterQuality),
            posterCardSize = prefs.enum(posterCardSizeKey, PosterCardSize.STANDARD),
            showTopTitleYear = prefs[showTopTitleYearKey] ?: false,
            showLibraryTitleYear = prefs[showLibraryTitleYearKey] ?: false,
            libraryContinueWatchingCardSize = prefs.enum(
                libraryContinueWatchingCardSizeKey,
                LibraryContinueWatchingCardSize.LARGE,
            ),
            preferredPlayer = prefs.enum(preferredPlayerKey, PreferredPlayer.NONE),
            preferredVideoQuality = prefs.enum(
                preferredVideoQualityKey,
                PreferredVideoQuality.BEST,
            ),
            watchNextEnabled = prefs[watchNextEnabledKey] ?: true,
            previewCacheSize = (prefs[previewCacheSizeKey] ?: 100).coerceIn(50, 500),
            autoSkipOpeningsEndings = prefs[autoSkipOpeningsEndingsKey] ?: false,
            autoSkipDelaySeconds = (prefs[autoSkipDelaySecondsKey] ?: 5).coerceIn(1, 15),
            showOpeningOnTimeline = prefs[showOpeningOnTimelineKey] ?: false,
            autoPlayNextEpisode = prefs[autoPlayNextEpisodeKey] ?: false,
            nextEpisodeSwitchDelaySeconds =
                (prefs[nextEpisodeSwitchDelaySecondsKey] ?: 10).coerceIn(0, 30),
            askDubbingOnWatch = prefs[askDubbingOnWatchKey] ?: false,
            pictureInPictureEnabled = prefs[pictureInPictureEnabledKey] ?: true,
            playerOrientationMode = prefs.enum(
                playerOrientationModeKey,
                PlayerOrientationMode.SYSTEM,
            ),
            suggestNextEpisodeOnWatched = prefs[suggestNextEpisodeOnWatchedKey] ?: true,
            watchedThresholds = prefs.watchedThresholds(),
            refreshContinueWatchingProgressOnLaunch =
                prefs[refreshContinueWatchingProgressOnLaunchKey] ?: false,
            tvPlayerVolumeKeysEnabled = prefs[tvPlayerVolumeKeysEnabledKey] ?: false,
            advancedPlayerVolumeEnabled = prefs[advancedPlayerVolumeEnabledKey] ?: false,
            volumeStabilizationEnabled = prefs[volumeStabilizationEnabledKey] ?: false,
            playerBufferProfile = prefs.enum(playerBufferProfileKey, PlayerBufferProfile.SMALL),
            videoExportAutoEnabled = prefs[videoExportAutoEnabledKey] ?: false,
            yaniApplicationToken = prefs.yaniApplicationToken(),
            contentLanguage = YaniContentLanguage.fromPreferenceValue(prefs[yaniContentLanguageKey])
                ?: store.resolveSystemContentLanguage(),
            detailsButtonOrder = prefs[detailsButtonOrderKey].toDetailsButtonOrder(),
        )
    }

    override val mainSettingsSnapshot: Flow<MainSettingsSnapshot> = store.data.map { prefs ->
        MainSettingsSnapshot(
            appTheme = prefs.enum(appThemeKey, AppTheme.WARM_AMBER),
            backgroundStyle = prefs.enum(backgroundStyleKey, BackgroundStyle.DARK),
            posterQuality = prefs.enum(posterQualityKey, defaultPosterQuality),
            posterCardSize = prefs.enum(posterCardSizeKey, PosterCardSize.STANDARD),
            yaniNickname = prefs[yaniNicknameKey].orEmpty(),
            yaniAvatarUrl = prefs[yaniAvatarUrlKey].orEmpty(),
            yaniUnreadNotificationsCount = prefs[yaniUnreadNotificationsCountKey] ?: 0,
        )
    }
}
