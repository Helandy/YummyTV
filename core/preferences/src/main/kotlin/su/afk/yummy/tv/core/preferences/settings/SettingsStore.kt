package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow

data class SettingsSnapshot(
    val appTheme: AppTheme,
    val posterQuality: PosterQuality,
    val posterCardSize: PosterCardSize,
    val showTopTitleYear: Boolean,
    val libraryContinueWatchingCardSize: LibraryContinueWatchingCardSize,
    val preferredPlayer: PreferredPlayer,
    val preferredVideoQuality: PreferredVideoQuality,
    val watchNextEnabled: Boolean,
    val previewCacheSize: PreviewCacheSize,
    val autoSkipOpeningsEndings: Boolean,
    val autoPlayNextEpisode: Boolean,
    val pictureInPictureEnabled: Boolean,
    val suggestNextEpisodeOnWatched: Boolean,
    val refreshContinueWatchingProgressOnLaunch: Boolean,
    val yaniApplicationToken: String,
    val contentLanguage: YaniContentLanguage,
    val detailsButtonOrder: List<DetailsButtonAction>,
)

data class MainSettingsSnapshot(
    val appTheme: AppTheme,
    val posterQuality: PosterQuality,
    val posterCardSize: PosterCardSize,
    val yaniNickname: String,
    val yaniAvatarUrl: String,
    val yaniUnreadNotificationsCount: Int,
)

data class SupportPromptSnapshot(
    val dismissed: Boolean,
    val firstEligibleTimeMs: Long,
)

interface SettingsStore {

    val currentPreviewCacheSize: PreviewCacheSize

    val posterQuality: Flow<PosterQuality>
    val posterCardSize: Flow<PosterCardSize>
    val showTopTitleYear: Flow<Boolean>
    val libraryContinueWatchingCardSize: Flow<LibraryContinueWatchingCardSize>
    val preferredPlayer: Flow<PreferredPlayer>
    val preferredVideoQuality: Flow<PreferredVideoQuality>
    val watchNextEnabled: Flow<Boolean>
    val previewCacheSize: Flow<PreviewCacheSize>
    val autoSkipOpeningsEndings: Flow<Boolean>
    val autoPlayNextEpisode: Flow<Boolean>
    val pictureInPictureEnabled: Flow<Boolean>
    val suggestNextEpisodeOnWatched: Flow<Boolean>
    val refreshContinueWatchingProgressOnLaunch: Flow<Boolean>
    val mobilePlayerGestureTutorialDismissed: Flow<Boolean>
    val mobilePlayerVolumePercent: Flow<Int>
    val playerResizeMode: Flow<PlayerResizeMode>
    val playerZoomLevel: Flow<PlayerZoomLevel>
    val appTheme: Flow<AppTheme>
    val detailsButtonOrder: Flow<List<DetailsButtonAction>>
    val yaniApplicationToken: Flow<String>
    val yaniApplicationTokenState: Flow<YaniApplicationTokenState>
    val yaniUserId: Flow<Int>
    val yaniNickname: Flow<String>
    val yaniAvatarUrl: Flow<String>
    val yaniTokenRefreshAt: Flow<Long>
    val yaniUnreadNotificationsCount: Flow<Int>
    val yaniContentLanguage: Flow<YaniContentLanguage>
    val settingsSnapshot: Flow<SettingsSnapshot>
    val mainSettingsSnapshot: Flow<MainSettingsSnapshot>
    val supportPromptSnapshot: Flow<SupportPromptSnapshot>

    fun playerResizeSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Flow<PlayerResizeSettings>

    fun playerMobileVideoTransformSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
    ): Flow<PlayerMobileVideoTransformSettings>

    suspend fun setPosterQuality(quality: PosterQuality)
    suspend fun setPosterCardSize(size: PosterCardSize)
    suspend fun setShowTopTitleYear(enabled: Boolean)
    suspend fun setLibraryContinueWatchingCardSize(size: LibraryContinueWatchingCardSize)
    suspend fun setPreferredPlayer(player: PreferredPlayer)
    suspend fun setPreferredVideoQuality(quality: PreferredVideoQuality)
    suspend fun setWatchNextEnabled(enabled: Boolean)
    suspend fun setPreviewCacheSize(size: PreviewCacheSize)
    suspend fun setAutoSkipOpeningsEndings(enabled: Boolean)
    suspend fun setAutoPlayNextEpisode(enabled: Boolean)
    suspend fun setPictureInPictureEnabled(enabled: Boolean)
    suspend fun setSuggestNextEpisodeOnWatched(enabled: Boolean)
    suspend fun setRefreshContinueWatchingProgressOnLaunch(enabled: Boolean)
    suspend fun dismissMobilePlayerGestureTutorial()
    suspend fun resetMobilePlayerGestureTutorial()
    suspend fun setMobilePlayerVolumePercent(percent: Int)
    suspend fun setPlayerResizeMode(mode: PlayerResizeMode)
    suspend fun setPlayerZoomLevel(level: PlayerZoomLevel)

    suspend fun setPlayerResizeSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
        settings: PlayerResizeSettings,
    )

    suspend fun setPlayerMobileVideoTransformSettings(
        animeId: Int,
        animeTitle: String,
        playerName: String,
        settings: PlayerMobileVideoTransformSettings,
    )

    suspend fun setAppTheme(theme: AppTheme)
    suspend fun setDetailsButtonOrder(order: List<DetailsButtonAction>)
    suspend fun setYaniApplicationToken(token: String)

    suspend fun setYaniAccount(
        userId: Int,
        nickname: String,
        avatarUrl: String?,
        refreshedAt: Long = System.currentTimeMillis(),
    )

    suspend fun clearLegacyYaniAccessToken()
    suspend fun clearYaniAccount()
    suspend fun setYaniUnreadNotificationsCount(count: Int)
    suspend fun setYaniContentLanguage(language: YaniContentLanguage)
    suspend fun ensureYaniContentLanguageInitialized()
    suspend fun ensureSupportPromptInstallTimeInitialized()
    suspend fun dismissSupportPrompt()

    /** Returns `true` when [versionCode] differs from the previously started one. */
    suspend fun markStartedVersion(versionCode: Int): Boolean

    /**
     * Returns `true` exactly once: the first time this is called after the streaming/download
     * cache split, so callers can prune the now-unbounded-legacy entries a single time. Every
     * later call returns `false`.
     */
    suspend fun consumeLegacyStreamingCachePruneFlag(): Boolean

    companion object {
        val defaultDetailsButtonOrder: List<DetailsButtonAction> = listOf(
            DetailsButtonAction.WATCH,
            DetailsButtonAction.LIBRARY,
            DetailsButtonAction.FAVORITE,
            DetailsButtonAction.EPISODES,
            DetailsButtonAction.FULL_DETAILS,
            DetailsButtonAction.SUBSCRIPTIONS,
            DetailsButtonAction.TRAILERS,
            DetailsButtonAction.SIMILAR,
            DetailsButtonAction.VIEWING_ORDER,
            DetailsButtonAction.RATING,
            DetailsButtonAction.COLLECTIONS,
            DetailsButtonAction.REVIEWS,
            DetailsButtonAction.BLOGGER_VIDEOS,
            DetailsButtonAction.SCREENSHOTS,
        )
    }
}
