package su.afk.yummy.tv.feature.settings

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.core.preferences.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.preferences.settings.PreviewCacheSize
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage

enum class DetailsButtonMoveDirection {
    UP,
    DOWN,
}

class SettingsState {
    data class State(
        val interfaceMode: AppInterfaceMode = AppInterfaceMode.MOBILE,
        val appTheme: AppTheme = AppTheme.WARM_AMBER,
        val posterQuality: PosterQuality = PosterQuality.STANDARD,
        val posterCardSize: PosterCardSize = PosterCardSize.STANDARD,
        val showTopTitleYear: Boolean = false,
        val libraryContinueWatchingCardSize: LibraryContinueWatchingCardSize =
            LibraryContinueWatchingCardSize.LARGE,
        val preferredPlayer: PreferredPlayer = PreferredPlayer.NONE,
        val preferredVideoQuality: PreferredVideoQuality = PreferredVideoQuality.BEST,
        val isPreviewChannelBrowsable: Boolean = false,
        val watchNextEnabled: Boolean = true,
        val previewCacheSize: PreviewCacheSize = PreviewCacheSize.MB_100,
        val autoSkipOpeningsEndings: Boolean = false,
        val autoPlayNextEpisode: Boolean = false,
        val pictureInPictureEnabled: Boolean = true,
        val mobilePlayerGestureTutorialDismissed: Boolean = false,
        val suggestNextEpisodeOnWatched: Boolean = true,
        val refreshContinueWatchingProgressOnLaunch: Boolean = false,
        val tvPlayerVolumeKeysEnabled: Boolean = false,
        val videoExportAutoEnabled: Boolean = false,
        val yaniApplicationToken: String = "",
        val contentLanguage: YaniContentLanguage = YaniContentLanguage.DEFAULT,
        val detailsButtonOrder: List<DetailsButtonAction> = SettingsStore.defaultDetailsButtonOrder,
        val videoExportDirectoryName: String? = null,
    ) : UiState

    /** Пользовательские действия на экране настроек. */
    sealed interface Event : UiEvent {
        /** Пользователь нажал кнопку возврата. */
        data object BackSelected : Event

        /** Пользователь выбрал тему приложения. */
        data class AppThemeSelected(val theme: AppTheme) : Event

        /** Пользователь подтвердил смену типа интерфейса. */
        data class InterfaceModeSelected(val mode: AppInterfaceMode) : Event

        /** Пользователь выбрал качество постеров. */
        data class PosterQualitySelected(val quality: PosterQuality) : Event

        /** Пользователь выбрал размер карточек постеров. */
        data class PosterCardSizeSelected(val size: PosterCardSize) : Event

        /** Пользователь переключил отображение года у тайтлов в топе. */
        data object ShowTopTitleYearToggled : Event

        /** Пользователь выбрал размер карточек продолжения просмотра в библиотеке. */
        data class LibraryContinueWatchingCardSizeSelected(
            val size: LibraryContinueWatchingCardSize,
        ) : Event

        /** Пользователь выбрал предпочитаемый плеер. */
        data class PreferredPlayerSelected(val player: PreferredPlayer) : Event

        /** Пользователь выбрал предпочитаемое качество видео. */
        data class PreferredVideoQualitySelected(val quality: PreferredVideoQuality) : Event

        /** Пользователь запросил доступность preview-канала на TV. */
        data object RequestPreviewChannelBrowsable : Event

        /** Пользователь переключил публикацию в Watch Next. */
        data object WatchNextToggled : Event

        /** Пользователь выбрал размер кеша превью. */
        data class PreviewCacheSizeSelected(val size: PreviewCacheSize) : Event

        /** Пользователь переключил автопропуск опенингов и эндингов. */
        data object AutoSkipOpeningsEndingsToggled : Event

        /** Пользователь переключил автовоспроизведение следующей серии. */
        data object AutoPlayNextEpisodeToggled : Event

        /** Пользователь включил или выключил плавающий режим мобильного плеера. */
        data object PictureInPictureToggled : Event

        /** Пользователь переключил перехват кнопок громкости в ТВ-плеере. */
        data object TvPlayerVolumeKeysToggled : Event

        /** Пользователь запросил повторный показ обучения жестам мобильного плеера. */
        data object MobilePlayerGestureTutorialReset : Event

        /** Пользователь переключил предложение следующей серии после завершения текущей. */
        data object SuggestNextEpisodeOnWatchedToggled : Event

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

        /** Пользователь сбросил порядок кнопок деталей к стандартному. */
        data object DetailsButtonOrderReset : Event

        data object VideoExportDirectorySelected : Event

        /** Пользователь переключил автоматическую выгрузку серии после скачивания. */
        data object VideoExportAutoToggled : Event
        data class VideoExportDirectoryGranted(val uri: String) : Event
    }

    sealed interface Effect : UiEffect {
        /** Сохранён новый тип интерфейса, приложение нужно запустить заново. */
        data object RestartApplication : Effect
        data object OpenVideoExportDirectoryPicker : Effect
        data object VideoExportDirectorySelectionFailed : Effect
    }
}
