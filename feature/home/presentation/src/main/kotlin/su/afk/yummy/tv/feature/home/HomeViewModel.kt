package su.afk.yummy.tv.feature.home

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.settings.HomeBlockKind
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.preferences.settings.contentLanguageChanges
import su.afk.yummy.tv.core.tv.api.ITvIntegration
import su.afk.yummy.tv.core.utils.formatting.millisToClockTime
import su.afk.yummy.tv.core.utils.system.BOOSTY_URL
import su.afk.yummy.tv.core.utils.system.COMMUNITY_TELEGRAM_URL
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideosUseCase
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.usecase.GetCachedHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.GetRecentlyAiredScheduleUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveLibraryNewEpisodeAnimeIdsUseCase
import su.afk.yummy.tv.domain.home.usecase.ObserveWatchedEpisodesUseCase
import su.afk.yummy.tv.domain.home.usecase.RefreshHomeFeedUseCase
import su.afk.yummy.tv.domain.home.usecase.RefreshRemoteContinueWatchingUseCase
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.catalog.ICatalogSearchOpener
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.handler.HomeAnnouncementHandler
import su.afk.yummy.tv.feature.home.handler.HomeContinueWatchingHandler
import su.afk.yummy.tv.feature.home.handler.HomePromptsHandler
import su.afk.yummy.tv.feature.home.handler.HomeRecommendationHandler
import su.afk.yummy.tv.feature.home.model.HomeAnnouncementResult
import su.afk.yummy.tv.feature.home.model.HomeFeedVisibilityParams
import su.afk.yummy.tv.feature.home.model.PromptSchedule
import su.afk.yummy.tv.feature.home.navigator.HomeRecommendationsDestination
import su.afk.yummy.tv.feature.home.presentation.R
import su.afk.yummy.tv.feature.home.utils.enabledKinds
import su.afk.yummy.tv.feature.home.utils.hasPlayableTarget
import su.afk.yummy.tv.feature.home.utils.shouldOfferPreviewChannel
import su.afk.yummy.tv.feature.home.utils.toVisibleFeed
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.getPlayerDest
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator
import javax.inject.Inject
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val collectionNavigator: ICollectionNavigator,
    private val reviewsNavigator: IReviewsNavigator,
    private val bloggerVideosNavigator: IBloggerVideosNavigator,
    private val scheduleNavigator: IScheduleNavigator,
    private val catalogSearchOpener: ICatalogSearchOpener,
    private val getHomeFeed: GetHomeFeedUseCase,
    private val getBloggerVideos: GetBloggerVideosUseCase,
    private val getCachedHomeFeed: GetCachedHomeFeedUseCase,
    private val refreshHomeFeed: RefreshHomeFeedUseCase,
    private val refreshRemoteContinueWatching: RefreshRemoteContinueWatchingUseCase,
    private val observeLibraryNewEpisodeAnimeIds: ObserveLibraryNewEpisodeAnimeIdsUseCase,
    private val observeWatchedEpisodes: ObserveWatchedEpisodesUseCase,
    private val getRecentlyAiredSchedule: GetRecentlyAiredScheduleUseCase,
    private val appClock: AppClock,
    private val stringProvider: StringProvider,
    private val playerNavigator: IPlayerNavigator,
    private val settingsStore: SettingsStore,
    private val tvIntegration: ITvIntegration,
    private val analytics: HomeAnalytics,
    private val continueWatchingHandler: HomeContinueWatchingHandler,
    private val recommendationHandler: HomeRecommendationHandler,
    private val promptsHandler: HomePromptsHandler,
    private val announcementHandler: HomeAnnouncementHandler,
) : BaseViewModel<HomeState.State, HomeState.Event, HomeState.Effect>() {

    /** Пустое состояние до первой загрузки: лента ещё не пришла, показывается лоадер. */
    override fun createInitialState() = HomeState.State()

    private var supportPromptDisplayedThisSession = false

    /** Срок приглашения в сообщество наступил и оно ещё не показано — ждёт свободного экрана. */
    private var communityPromptDue = false

    /** Срок предложения Boosty наступил и оно ещё не показано — ждёт свободного экрана. */
    private var boostyPromptDue = false

    /** Ответ на предложение preview-канала не пришёл из хранилища — до него не показываем. */
    private var previewChannelPromptAnswered = true
    private var previewChannelBrowsable = false

    /** Лента без скрытых рекомендаций — из неё тайтл возвращается при откате. */
    private var rawFeed: HomeFeed? = null

    /** Зерно порядка «Аниме сезона»: новое на каждый заход, но стабильное, пока жива VM. */
    private val heroShuffleSeed = Random.nextLong()

    /** Всё, из чего к ленте добавляется секция новых серий. */
    private var feedParams = HomeFeedVisibilityParams()

    private var loadJob: Job? = null

    /** «Продолжить просмотр» как пришло из источника, до скрытия ожидающих удаления. */
    private var rawContinueWatching: List<HomeContinueWatchingItem> = emptyList()

    /** Отложенные удаления из «Продолжить просмотр»: id тайтла → таймер окна отмены. */
    private val continueWatchingRemovalJobs = mutableMapOf<Int, Job>()

    init {
        analytics.eventScreenOpened()
        continueWatchingHandler.observe()
            .onEach { items ->
                rawContinueWatching = items
                setState {
                    copy(
                        continueWatching = visibleContinueWatching(pendingContinueWatchingRemovalIds),
                        isContinueWatchingLoaded = true,
                    )
                }
                updatePreviewChannelPrompt()
            }
            .launchIn(viewModelScope)
        observeLibraryAnimeIds()
        loadRecentlyAired()
        observeHiddenRecommendations()
        observeSupportPrompt()
        observeAnnouncement()
        observePreviewChannelPrompt()
        observeCommunityPrompt()
        observeBoostyPrompt()
        loadBloggerVideos()
        load()
        reloadOnContentLanguageChange()
    }

    /** Лента кэшируется и запрашивается на языке контента, поэтому при его смене грузим её заново. */
    private fun reloadOnContentLanguageChange() {
        settingsStore.contentLanguageChanges()
            .onEach {
                setState { copy(feed = null, isLoading = true, error = null) }
                load()
            }
            .launchIn(viewModelScope)
    }

    /** Разбирает действия пользователя на главной: переходы, обновление, скрытие рекомендаций. */
    override fun onEvent(event: HomeState.Event) {
        when (event) {
            is HomeState.Event.AnimeSelected -> {
                analytics.eventAnimeSelected(event.seriesId)
                nav.navigate(detailsNavigator.getDetailsDest(event.seriesId))
            }

            is HomeState.Event.CollectionSelected -> {
                analytics.eventCollectionSelected(event.collectionId)
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            }

            is HomeState.Event.ContinueWatchingSelected -> {
                analytics.eventContinueWatchingSelected(event.entry)
                launchContinueWatching(event.entry)
            }

            HomeState.Event.RetrySelected -> {
                analytics.eventRetry()
                load()
            }

            HomeState.Event.ScreenResumed -> {
                refreshRemoteContinueWatching()
                syncCachedContinueWatching()
                // На ТВ главная живёт в стеке и не пересоздаётся — без этого «Новые серии» не обновятся.
                loadRecentlyAired()
            }

            HomeState.Event.RefreshRequested -> refresh()

            HomeState.Event.CollectionsCatalogSelected -> {
                nav.navigate(collectionNavigator.getCollectionsCatalogDest())
            }

            HomeState.Event.RecommendationsCatalogSelected -> {
                nav.navigate(HomeRecommendationsDestination)
            }

            HomeState.Event.ScheduleSelected -> {
                nav.navigate(scheduleNavigator.getScheduleDest())
            }

            HomeState.Event.ReviewsSelected -> {
                nav.navigate(reviewsNavigator.feed())
            }

            HomeState.Event.SearchSelected -> catalogSearchOpener.openSearch()

            HomeState.Event.BloggerVideosSelected -> nav.navigate(bloggerVideosNavigator.feed())

            HomeState.Event.BloggerVideosRetrySelected -> loadBloggerVideos()

            is HomeState.Event.BloggerVideoSelected ->
                nav.navigate(bloggerVideosNavigator.video(event.video.id))

            HomeState.Event.SupportPromptDismissed -> dismissSupportPrompt()
            HomeState.Event.CommunityPromptDismissed -> hideCommunityPrompt()
            HomeState.Event.CommunityPromptJoinClicked -> {
                hideCommunityPrompt()
                setEffect(HomeState.Effect.OpenUri(COMMUNITY_TELEGRAM_URL))
            }
            HomeState.Event.BoostyPromptDismissed -> hideBoostyPrompt()
            HomeState.Event.BoostyPromptSupportClicked -> {
                hideBoostyPrompt()
                setEffect(HomeState.Effect.OpenUri(BOOSTY_URL))
            }
            HomeState.Event.PreviewChannelPromptAccepted -> answerPreviewChannelPrompt(accepted = true)
            HomeState.Event.PreviewChannelPromptDismissed -> answerPreviewChannelPrompt(accepted = false)

            HomeState.Event.AnnouncementDismissed -> dismissAnnouncement()

            is HomeState.Event.RecommendationHideRequested ->
                setRecommendationHidden(event.animeId, hidden = true)

            is HomeState.Event.RecommendationRestoreRequested ->
                setRecommendationHidden(event.animeId, hidden = false)

            is HomeState.Event.ContinueWatchingRemoveRequested ->
                requestContinueWatchingRemoval(event.entry)

            is HomeState.Event.ContinueWatchingRemoveUndone ->
                undoContinueWatchingRemoval(event.animeId)
        }
    }

    private fun visibleContinueWatching(hiddenIds: Set<Int>) =
        rawContinueWatching.filterNot { it.animeId in hiddenIds }.toImmutableList()

    /**
     * Сразу прячет тайтл из «Продолжить просмотр», а насовсем убирает только по истечении окна
     * отмены: восстановить уже подавленную запись нечем.
     */
    private fun requestContinueWatchingRemoval(entry: HomeContinueWatchingItem) {
        val animeId = entry.animeId
        if (animeId in continueWatchingRemovalJobs) return
        setContinueWatchingRemovalPending(animeId, pending = true)
        continueWatchingRemovalJobs[animeId] = viewModelScope.launch {
            continueWatchingHandler.commitRemoval(animeId)
            continueWatchingRemovalJobs.remove(animeId)
            setContinueWatchingRemovalPending(animeId, pending = false)
        }
        setEffect(
            HomeState.Effect.ShowContinueWatchingUndo(
                message = stringProvider.get(R.string.home_continue_watching_removed),
                animeId = animeId,
            ),
        )
    }

    private fun undoContinueWatchingRemoval(animeId: Int) {
        continueWatchingRemovalJobs.remove(animeId)?.cancel() ?: return
        setContinueWatchingRemovalPending(animeId, pending = false)
    }

    private fun setContinueWatchingRemovalPending(animeId: Int, pending: Boolean) {
        setState {
            val ids = if (pending) {
                pendingContinueWatchingRemovalIds.add(animeId)
            } else {
                pendingContinueWatchingRemovalIds.remove(animeId)
            }
            copy(
                pendingContinueWatchingRemovalIds = ids,
                continueWatching = visibleContinueWatching(ids),
            )
        }
    }

    /** Следит за списками пользователя — по ним отбираются тайтлы для блока новых серий. */
    private fun observeLibraryAnimeIds() {
        settingsStore.newEpisodesSources
            .flatMapLatest { sources -> observeLibraryNewEpisodeAnimeIds(sources) }
            .onEach { ids ->
                feedParams = feedParams.copy(libraryAnimeIds = ids)
                applyHiddenRecommendations()
            }
            .launchIn(viewModelScope)

        settingsStore.homeBlocks
            .onEach { blocks ->
                feedParams = feedParams.copy(
                    newEpisodesEnabled = blocks.any { it.kind == HomeBlockKind.MY_NEW_EPISODES && it.enabled },
                )
                setState { copy(blocks = blocks.enabledKinds()) }
                applyHiddenRecommendations()
            }
            .launchIn(viewModelScope)

        observeWatchedEpisodes()
            .onEach { watched ->
                feedParams = feedParams.copy(watchedEpisodes = watched)
                applyHiddenRecommendations()
            }
            .launchIn(viewModelScope)
    }

    /** Расписание кэшируется на час, поэтому лишней нагрузки на API это не даёт; ошибка не мешает главной. */
    private fun loadRecentlyAired() {
        viewModelScope.launch {
            runSuspendCatching { getRecentlyAiredSchedule(appClock.nowMillis() / 1_000L) }
                .onSuccess { items ->
                    feedParams = feedParams.copy(recentlyAired = items)
                    applyHiddenRecommendations()
                }
        }
    }

    // Скрытия приходят и с других экранов («Похожее» в деталях), поэтому набор берём из стора.
    private fun observeHiddenRecommendations() {
        settingsStore.hiddenRecommendationIds
            .onEach { hiddenIds ->
                setState { copy(hiddenRecommendationIds = hiddenIds.toPersistentSet()) }
                applyHiddenRecommendations()
            }
            .launchIn(viewModelScope)
    }

    /** Скрывает тайтл из рекомендаций или возвращает обратно, откатываясь при отказе сервера. */
    private fun setRecommendationHidden(animeId: Int, hidden: Boolean) {
        if (animeId in currentState.pendingRecommendationIds) return
        viewModelScope.launch {
            if (!recommendationHandler.isAuthorized()) {
                setEffect(
                    HomeState.Effect.ShowToast(
                        stringProvider.get(R.string.home_recommendation_auth_required),
                    ),
                )
                return@launch
            }
            setState {
                copy(
                    hiddenRecommendationIds = if (hidden) {
                        hiddenRecommendationIds + animeId
                    } else {
                        hiddenRecommendationIds - animeId
                    },
                    pendingRecommendationIds = pendingRecommendationIds + animeId,
                )
            }
            applyHiddenRecommendations()
            if (recommendationHandler.setIgnored(animeId, hidden)) {
                if (hidden) {
                    analytics.eventRecommendationHidden(animeId)
                    setEffect(
                        HomeState.Effect.ShowRecommendationUndo(
                            message = stringProvider.get(R.string.home_recommendation_hidden),
                            animeId = animeId,
                        ),
                    )
                } else {
                    analytics.eventRecommendationRestored(animeId)
                }
                setState { copy(pendingRecommendationIds = pendingRecommendationIds - animeId) }
            } else {
                rollbackRecommendation(animeId, hidden)
            }
        }
    }

    /** Возвращает тайтл в исходное состояние, если сервер не принял изменение. */
    private fun rollbackRecommendation(animeId: Int, hidden: Boolean) {
        setState {
            copy(
                hiddenRecommendationIds = if (hidden) {
                    hiddenRecommendationIds - animeId
                } else {
                    hiddenRecommendationIds + animeId
                },
                pendingRecommendationIds = pendingRecommendationIds - animeId,
            )
        }
        applyHiddenRecommendations()
        setEffect(
            HomeState.Effect.ShowToast(
                stringProvider.get(R.string.home_recommendation_error),
            ),
        )
    }

    /** Пересобирает видимую ленту из [rawFeed] с учётом скрытых рекомендаций. */
    private fun applyHiddenRecommendations() {
        val feed = rawFeed ?: return
        val hiddenIds = currentState.hiddenRecommendationIds
        setState {
            copy(feed = feed.toVisibleFeed(hiddenIds))
        }
    }

    /** Следит за тем, когда окно поддержки проекта станет доступно к показу. */
    private fun observeSupportPrompt() {
        viewModelScope.launch { promptsHandler.ensureSupportInitialized() }
        promptsHandler.supportSchedule()
            .onEach(::applySupportPromptSchedule)
            .launchIn(viewModelScope)
    }

    private fun applySupportPromptSchedule(schedule: PromptSchedule) {
        when (schedule) {
            PromptSchedule.Dismissed -> if (!supportPromptDisplayedThisSession) {
                setState { copy(supportPromptVisible = false) }
            }

            PromptSchedule.Waiting -> setState { copy(supportPromptVisible = false) }
            PromptSchedule.Due -> showSupportPromptOnce()
        }
    }

    /** Показывает окно поддержки один раз за сессию и сразу помечает его отработавшим. */
    private fun showSupportPromptOnce() {
        supportPromptDisplayedThisSession = true
        setState { copy(supportPromptVisible = true) }
        updatePreviewChannelPrompt()
        viewModelScope.launch { promptsHandler.markSupportHandled() }
    }

    /** Закрывает окно поддержки и запоминает отказ, чтобы больше не показывать. */
    private fun dismissSupportPrompt() {
        setState { copy(supportPromptVisible = false) }
        updatePreviewChannelPrompt()
        viewModelScope.launch { promptsHandler.markSupportHandled() }
    }

    /** Следит за тем, когда приглашение в сообщество станет доступно к показу. */
    private fun observeCommunityPrompt() {
        viewModelScope.launch { promptsHandler.ensureCommunityInitialized() }
        promptsHandler.communitySchedule()
            .onEach { schedule ->
                communityPromptDue = schedule == PromptSchedule.Due
                if (communityPromptDue) updateCommunityPrompt()
            }
            .launchIn(viewModelScope)
    }

    /** Показывает приглашение один раз, когда оно созрело и поверх главной нет других диалогов. */
    private fun updateCommunityPrompt() {
        if (!communityPromptDue || currentState.communityPromptVisible) return
        if (currentState.supportPromptVisible ||
            currentState.boostyPromptVisible ||
            currentState.announcement != null ||
            currentState.previewChannelPromptVisible
        ) {
            return
        }
        communityPromptDue = false
        setState { copy(communityPromptVisible = true) }
        viewModelScope.launch { promptsHandler.markCommunityHandled() }
    }

    /** Следит за тем, когда предложение поддержать на Boosty станет доступно к показу. */
    private fun observeBoostyPrompt() {
        promptsHandler.boostySchedule()
            .onEach { schedule ->
                boostyPromptDue = schedule == PromptSchedule.Due
                if (boostyPromptDue) updateBoostyPrompt()
            }
            .launchIn(viewModelScope)
    }

    /** Показывает предложение один раз, когда оно созрело и поверх главной нет других диалогов. */
    private fun updateBoostyPrompt() {
        if (!boostyPromptDue || currentState.boostyPromptVisible) return
        if (currentState.supportPromptVisible ||
            currentState.communityPromptVisible ||
            currentState.announcement != null ||
            currentState.previewChannelPromptVisible
        ) {
            return
        }
        boostyPromptDue = false
        setState { copy(boostyPromptVisible = true) }
        viewModelScope.launch { promptsHandler.markBoostyHandled() }
    }

    /** Закрывает предложение; показанным оно уже помечено при появлении. */
    private fun hideBoostyPrompt() {
        setState { copy(boostyPromptVisible = false) }
        updatePreviewChannelPrompt()
    }

    /** Закрывает приглашение; показанным оно уже помечено при появлении. */
    private fun hideCommunityPrompt() {
        setState { copy(communityPromptVisible = false) }
        updatePreviewChannelPrompt()
    }

    /** Следит за ответом пользователя и статусом preview-канала; только на телевизоре. */
    private fun observePreviewChannelPrompt() {
        if (!tvIntegration.isSupported) return
        combine(
            settingsStore.previewChannelPromptAnswered,
            tvIntegration.previewChannelBrowsable,
        ) { answered, browsable -> answered to browsable }
            .onEach { (answered, browsable) ->
                previewChannelPromptAnswered = answered
                previewChannelBrowsable = browsable
                updatePreviewChannelPrompt()
            }
            .launchIn(viewModelScope)
    }

    /** Показывает предложение, когда оно уместно и не перекрывает другие диалоги главной. */
    private fun updatePreviewChannelPrompt() {
        val visible = shouldOfferPreviewChannel(
            isSupported = tvIntegration.isSupported,
            answered = previewChannelPromptAnswered,
            browsable = previewChannelBrowsable,
            hasContinueWatching = currentState.continueWatching.isNotEmpty(),
            otherDialogVisible = currentState.supportPromptVisible ||
                currentState.communityPromptVisible ||
                currentState.boostyPromptVisible ||
                currentState.announcement != null,
        )
        if (visible != currentState.previewChannelPromptVisible) {
            setState { copy(previewChannelPromptVisible = visible) }
        }
        updateCommunityPrompt()
        updateBoostyPrompt()
    }

    /** Запоминает ответ; при согласии открывает системный запрос на добавление канала. */
    private fun answerPreviewChannelPrompt(accepted: Boolean) {
        previewChannelPromptAnswered = true
        setState { copy(previewChannelPromptVisible = false) }
        if (accepted) tvIntegration.requestPreviewChannelBrowsable()
        viewModelScope.launch {
            settingsStore.markPreviewChannelPromptAnswered()
        }
    }

    /** Перепроверяет объявление при старте и после каждого обновления фичефлагов. */
    private fun observeAnnouncement() {
        checkAnnouncement()
        announcementHandler.updates()
            .onEach { checkAnnouncement() }
            .launchIn(viewModelScope)
    }

    /** Показывает объявление, если оно включено и ещё не было прочитано. */
    private fun checkAnnouncement() {
        setState { copy(statusMessage = announcementHandler.statusMessage()) }
        viewModelScope.launch {
            when (val result = announcementHandler.resolve()) {
                HomeAnnouncementResult.Disabled -> setState { copy(announcement = null) }
                HomeAnnouncementResult.AlreadySeen -> Unit
                is HomeAnnouncementResult.Show -> {
                    setState { copy(announcement = result.announcement) }
                    updatePreviewChannelPrompt()
                }
            }
        }
    }

    /** Закрывает объявление и запоминает его прочитанным. */
    private fun dismissAnnouncement() {
        val id = currentState.announcement?.id ?: return
        setState { copy(announcement = null) }
        updatePreviewChannelPrompt()
        viewModelScope.launch { announcementHandler.markSeen(id) }
    }

    /**
     * Открывает плеер по записи «продолжить просмотр», а без цели для запуска — детали тайтла.
     * Если предложенной следующей серии нет, сообщает об этом и тоже открывает детали.
     */
    private fun launchContinueWatching(entry: HomeContinueWatchingItem) {
        if (!entry.hasPlayableTarget()) {
            nav.navigate(detailsNavigator.getDetailsDest(entry.animeId))
            return
        }
        if (currentState.launchingContinueWatchingAnimeId != null) return
        setState { copy(launchingContinueWatchingAnimeId = entry.animeId) }
        viewModelScope.launch {
            try {
                val result = continueWatchingHandler.resolveLaunch(entry)
                if (result == null) {
                    setEffect(
                        HomeState.Effect.ShowToast(
                            stringProvider.get(R.string.home_next_episode_unavailable_toast, entry.episode),
                        ),
                    )
                    nav.navigate(detailsNavigator.getDetailsDest(entry.animeId))
                    return@launch
                }
                result.remoteProgressSwitch?.let { progress ->
                    setEffect(
                        HomeState.Effect.ShowToast(
                            stringProvider.get(
                                R.string.home_remote_continue_progress_toast,
                                progress.episode,
                                progress.positionMs.millisToClockTime(),
                            ),
                        ),
                    )
                }
                nav.navigate(playerNavigator.getPlayerDest(result))
            } finally {
                setState { copy(launchingContinueWatchingAnimeId = null) }
            }
        }
    }

    /** Грузит ленту: сперва отдаёт кэш, чтобы не держать лоадер, затем обновляет из сети. */
    private fun load() {
        refreshRemoteContinueWatching()
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val cachedFeed = if (currentState.feed == null) {
                runSuspendCatching { getCachedHomeFeed() }.getOrNull()
            } else {
                null
            }
            if (cachedFeed != null) {
                setState { copy(error = null) }
                applyFeed(cachedFeed, isLoading = false)
            } else if (currentState.feed == null) {
                setState { copy(isLoading = true, error = null) }
            } else {
                setState { copy(error = null) }
            }
            runSuspendCatching { getHomeFeed() }.fold(
                onSuccess = { feed -> applyFeed(feed, isLoading = false) },
                onFailure = { e ->
                    analytics.eventLoadError(e)
                    setState {
                        copy(
                            isLoading = false,
                            error = if (feed == null) {
                                UiText.Failure(e, UiText.Res(R.string.home_load_error))
                            } else {
                                error
                            },
                        )
                    }
                },
            )
        }
    }

    /** Грузит блок видео блогеров; его ошибка живёт отдельно от ошибки ленты. */
    private fun loadBloggerVideos() {
        viewModelScope.launch {
            setState { copy(isBloggerVideosLoading = true, bloggerVideosError = null) }
            runSuspendCatching { getBloggerVideos(limit = 10) }.fold(
                onSuccess = { videos ->
                    setState {
                        copy(
                            bloggerVideos = videos.toImmutableList(),
                            isBloggerVideosLoading = false,
                        )
                    }
                },
                onFailure = { error ->
                    setState {
                        copy(
                            isBloggerVideosLoading = false,
                            bloggerVideosError = UiText.Failure(error, UiText.Res(R.string.home_blogger_videos_load_error)),
                        )
                    }
                },
            )
        }
    }

    /** Обновляет ленту по запросу пользователя, не стирая уже показанную при неудаче. */
    private fun refresh() {
        refreshRemoteContinueWatching()
        viewModelScope.launch {
            if (currentState.isLoading && currentState.feed == null) return@launch
            if (currentState.isRefreshing) return@launch
            val showInitialLoading = currentState.feed == null
            if (showInitialLoading) {
                setState { copy(isLoading = true, error = null) }
            } else {
                setState { copy(isRefreshing = true) }
            }
            loadRecentlyAired()
            val result = runSuspendCatching { refreshHomeFeed() }
            setState { copy(isRefreshing = false) }
            result.fold(
                onSuccess = { feed -> applyFeed(feed, isLoading = false) },
                onFailure = { e ->
                    analytics.eventLoadError(e)
                    if (!showInitialLoading) {
                        setEffect(
                            HomeState.Effect.ShowToast(stringProvider.get(R.string.home_refresh_error)),
                        )
                    }
                    if (currentState.feed == null) {
                        setState {
                            copy(
                                isLoading = false,
                                error = UiText.Failure(e, UiText.Res(R.string.home_load_error)),
                            )
                        }
                    } else if (showInitialLoading) {
                        setState { copy(isLoading = false) }
                    }
                },
            )
        }
    }

    /** Подтягивает прогресс просмотра из кэша при возврате на экран: он мог измениться в плеере. */
    private fun syncCachedContinueWatching() {
        if (currentState.feed == null) return
        viewModelScope.launch {
            val cachedFeed = runSuspendCatching { getCachedHomeFeed() }.getOrNull() ?: return@launch
            val currentFeed = rawFeed
            applyFeed(
                feed = currentFeed?.copy(continueWatchingItems = cachedFeed.continueWatchingItems)
                    ?: cachedFeed,
                isLoading = false,
            )
        }
    }

    /** Запоминает исходную ленту и кладёт в состояние её видимую версию. */
    private fun applyFeed(feed: HomeFeed, isLoading: Boolean) {
        rawFeed = feed
        setState {
            copy(
                isLoading = isLoading,
                feed = feed.toVisibleFeed(hiddenRecommendationIds),
            )
        }
    }

    private fun HomeFeed.toVisibleFeed(hiddenIds: Set<Int>): HomeFeed =
        toVisibleFeed(hiddenIds, heroShuffleSeed, feedParams) {
            stringProvider.get(R.string.home_section_my_new_episodes)
        }
}
