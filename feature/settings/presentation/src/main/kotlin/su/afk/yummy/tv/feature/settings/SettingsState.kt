package su.afk.yummy.tv.feature.settings

import android.os.Build
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.model.settings.AppTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleStyleSettings
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.WatchedThresholds
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.system.CacheStorageEntry
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.model.ReleaseNotesStatus
import su.afk.yummy.tv.feature.settings.navigator.SettingsCategory

class SettingsState {
    @Immutable
    data class State(
        val interfaceMode: AppInterfaceMode = AppInterfaceMode.MOBILE,
        val appTheme: AppTheme = AppTheme.WARM_AMBER,
        val backgroundStyle: BackgroundStyle = BackgroundStyle.DARK,
        val posterQuality: PosterQuality = PosterQuality.STANDARD,
        val posterCardSize: PosterCardSize = PosterCardSize.STANDARD,
        val showTopTitleYear: Boolean = false,
        val showLibraryTitleYear: Boolean = false,
        val libraryContinueWatchingCardSize: LibraryContinueWatchingCardSize =
            LibraryContinueWatchingCardSize.LARGE,
        val preferredPlayer: PreferredPlayer = PreferredPlayer.NONE,
        val preferredVideoQuality: PreferredVideoQuality = PreferredVideoQuality.BEST,
        val isPreviewChannelBrowsable: Boolean = false,
        val watchNextEnabled: Boolean = true,
        val previewCacheSize: Int = 100,
        val autoSkipOpeningsEndings: Boolean = false,
        val autoSkipDelaySeconds: Int = 5,
        val showOpeningOnTimeline: Boolean = false,
        val autoPlayNextEpisode: Boolean = false,
        val nextEpisodeSwitchDelaySeconds: Int = 10,
        val askDubbingOnWatch: Boolean = true,
        val pictureInPictureEnabled: Boolean = true,
        val playerOrientationMode: PlayerOrientationMode = PlayerOrientationMode.SYSTEM,
        val subtitleStyle: PlayerSubtitleStyleSettings = PlayerSubtitleStyleSettings(),
        val mobilePlayerGestureTutorialDismissed: Boolean = false,
        val tvPlayerControlsTutorialDismissed: Boolean = false,
        val suggestNextEpisodeOnWatched: Boolean = true,
        val watchedThresholds: WatchedThresholds = WatchedThresholds(),
        val refreshContinueWatchingProgressOnLaunch: Boolean = false,
        val tvPlayerVolumeKeysEnabled: Boolean = false,
        val advancedPlayerVolumeEnabled: Boolean = false,
        val volumeStabilizationEnabled: Boolean = false,
        val playerBufferProfile: PlayerBufferProfile = PlayerBufferProfile.SMALL,
        /** Сжатие динамического диапазона (DynamicsProcessing) доступно только с Android 9 (API 28). */
        val volumeStabilizationSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P,
        val videoExportAutoEnabled: Boolean = false,
        val yaniApplicationToken: String = "",
        val contentLanguage: YaniContentLanguage = YaniContentLanguage.DEFAULT,
        val detailsButtonOrder: ImmutableList<DetailsButtonAction> =
            SettingsStore.defaultDetailsButtonOrder.toImmutableList(),
        val videoExportDirectoryName: String? = null,
        val cacheStorageEntries: ImmutableList<CacheStorageEntry> = persistentListOf(),
        val cacheStorageTotalBytes: Long = 0L,
        val isCacheStorageLoading: Boolean = false,
        val saveLastSearchEnabled: Boolean = false,
        val betaUpdatesEnabled: Boolean = false,
        /** Токен сессии хранится без AndroidKeyStore — прошивка не даёт им пользоваться. */
        val isFallbackSessionStorage: Boolean = false,
        val releaseNotes: ReleaseNotesStatus = ReleaseNotesStatus.Idle,
    ) : UiState

    /** Пользовательские действия на экране настроек. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь выбрал тему приложения. */
        data class AppThemeSelected(val theme: AppTheme) : Event

        /** Пользователь выбрал цвет фона интерфейса. */
        data class BackgroundStyleSelected(val style: BackgroundStyle) : Event

        /** Пользователь подтвердил смену типа интерфейса. */
        data class InterfaceModeSelected(val mode: AppInterfaceMode) : Event

        /** Пользователь выбрал качество постеров. */
        data class PosterQualitySelected(val quality: PosterQuality) : Event

        /** Пользователь выбрал размер карточек постеров. */
        data class PosterCardSizeSelected(val size: PosterCardSize) : Event

        /** Пользователь переключил отображение года у тайтлов в топе. */
        data object ShowTopTitleYearToggled : Event

        /** Пользователь переключил отображение года у тайтлов в библиотеке. */
        data object ShowLibraryTitleYearToggled : Event

        /** Пользователь выбрал размер карточек продолжения просмотра в библиотеке. */
        data class LibraryContinueWatchingCardSizeSelected(
            val size: LibraryContinueWatchingCardSize,
        ) : Event

        /** Пользователь выбрал предпочитаемый плеер. */
        data class PreferredPlayerSelected(val player: PreferredPlayer) : Event

        /** Пользователь выбрал предпочитаемое качество видео. */
        data class PreferredVideoQualitySelected(val quality: PreferredVideoQuality) : Event

        /** Пользователь выбрал размер буфера плеера. */
        data class PlayerBufferProfileSelected(val profile: PlayerBufferProfile) : Event

        /** Пользователь запросил доступность preview-канала на TV. */
        data object RequestPreviewChannelBrowsable : Event

        /** Пользователь переключил публикацию в Watch Next. */
        data object WatchNextToggled : Event

        /** Пользователь выбрал размер кеша превью. */
        data class PreviewCacheSizeSelected(val size: Int) : Event

        /** Пользователь переключил автопропуск опенингов и эндингов. */
        data object AutoSkipOpeningsEndingsToggled : Event

        /** Пользователь изменил задержку перед автопропуском опенинга/эндинга. */
        data class AutoSkipDelayChanged(val seconds: Int) : Event

        /** Пользователь переключил отображение опенинга на полосе прогресса плеера. */
        data object ShowOpeningOnTimelineToggled : Event

        /** Пользователь переключил автовоспроизведение следующей серии. */
        data object AutoPlayNextEpisodeToggled : Event

        /** Пользователь изменил задержку перед авто-переключением на следующую серию. */
        data class NextEpisodeSwitchDelayChanged(val seconds: Int) : Event

        /** Пользователь переключил запрос выбора озвучки при нажатии "Смотреть". */
        data object AskDubbingOnWatchToggled : Event

        /** Пользователь включил или выключил плавающий режим мобильного плеера. */
        data object PictureInPictureToggled : Event

        /** Пользователь выбрал режим принудительной ориентации плеера. */
        data class PlayerOrientationModeSelected(val mode: PlayerOrientationMode) : Event

        /** Пользователь изменил оформление субтитров (размер, цвет, фон, отступ снизу). */
        data class SubtitleStyleSelected(val settings: PlayerSubtitleStyleSettings) : Event

        /** Пользователь переключил перехват кнопок громкости в ТВ-плеере. */
        data object TvPlayerVolumeKeysToggled : Event

        data object AdvancedPlayerVolumeToggled : Event

        /** Пользователь переключил стабилизацию громкости (сжатие динамического диапазона). */
        data object VolumeStabilizationToggled : Event

        /** Пользователь запросил повторный показ обучения жестам мобильного плеера. */
        data object MobilePlayerGestureTutorialReset : Event

        /** Пользователь запросил повторный показ обучения управлению ТВ-плеером. */
        data object TvPlayerControlsTutorialReset : Event

        /** Пользователь переключил предложение следующей серии после завершения текущей. */
        data object SuggestNextEpisodeOnWatchedToggled : Event

        /** Пользователь изменил пороги "просмотрено" (минуты до конца по длине серии). */
        data class WatchedThresholdsChanged(val thresholds: WatchedThresholds) : Event

        /** Пользователь переключил запрос последнего прогресса при запуске продолжения просмотра. */
        data object RefreshContinueWatchingProgressOnLaunchToggled : Event

        /** Пользователь изменил токен приложения Yani. */
        data class YaniApplicationTokenChanged(val token: String) : Event

        /** Пользователь выбрал язык контента Yani. */
        data class ContentLanguageSelected(val language: YaniContentLanguage) : Event

        /** Пользователь переместил кнопку деталей в указанном направлении. */
        data class DetailsButtonMoved(
            val action: DetailsButtonAction,
            val direction: DetailsButtonMoveDirection,
        ) : Event

        /** Пользователь открыл экран настройки порядка кнопок деталей. */
        data object DetailsButtonOrderScreenOpened : Event

        /** Пользователь подтвердил текущий порядок кнопок деталей. */
        data object DetailsButtonOrderSelected : Event

        /** Пользователь открыл категорию настроек (мобильный интерфейс). */
        data class CategorySelected(val category: SettingsCategory) : Event

        /** Передача сессии на ТВ по локальной сети. */
        data object LoginOnTvSelected : Event

        /** Пользователь сбросил порядок кнопок деталей к стандартному. */
        data object DetailsButtonOrderReset : Event

        data object VideoExportDirectorySelected : Event

        /** Пользователь переключил автоматическую выгрузку серии после скачивания. */
        data object VideoExportAutoToggled : Event
        data class VideoExportDirectoryGranted(val uri: String) : Event

        /** Пользователь запросил пересчёт размеров папок кэша. */
        data object CacheStorageRefreshRequested : Event

        /** Пользователь переключил сохранение последнего поиска. */
        data object SaveLastSearchToggled : Event
        data object BetaUpdatesToggled : Event

        /** Пользователь открыл «Что нового»: загрузить историю релизов, если её ещё нет. */
        data object ReleaseNotesRequested : Event
    }

    sealed interface Effect : UiEffect {
        /** Сохранён новый тип интерфейса, приложение нужно запустить заново. */
        data object RestartApplication : Effect
        data object OpenVideoExportDirectoryPicker : Effect
        data object VideoExportDirectorySelectionFailed : Effect
    }
}
