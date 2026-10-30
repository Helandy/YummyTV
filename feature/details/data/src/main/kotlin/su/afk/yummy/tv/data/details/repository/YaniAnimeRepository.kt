package su.afk.yummy.tv.data.details.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeEpisodeInfo
import su.afk.yummy.tv.core.model.anime.AnimeEpisodeFrames
import su.afk.yummy.tv.core.model.anime.AnimeTmdbImages
import su.afk.yummy.tv.core.model.anime.AnimeRecommendation
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationReaction
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationVote
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.currentLanguageCode
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.core.storage.anime.AnimeTrailersCache
import su.afk.yummy.tv.core.storage.anime.AnimeVideosCache
import su.afk.yummy.tv.core.storage.anime.isFresh
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.core.storage.document.getOrFetchJson
import su.afk.yummy.tv.core.storage.offlinefirst.offlineFirstCache
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.data.details.dto.MalEpisodesDto
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationItemDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationsDto
import su.afk.yummy.tv.data.details.dto.YummyEpisodesDto
import su.afk.yummy.tv.data.details.mapper.AnimeRelationCachePayload
import su.afk.yummy.tv.data.details.mapper.ExtraVideosCachePayload
import su.afk.yummy.tv.data.details.mapper.toAnimeDetails
import su.afk.yummy.tv.data.details.mapper.toAnimeEpisodeInfoByNumber
import su.afk.yummy.tv.data.details.mapper.toAnimeTmdbImages
import su.afk.yummy.tv.data.details.mapper.toOrderedFrameUrls
import su.afk.yummy.tv.data.details.mapper.toEpisodeFlagsByNumber
import su.afk.yummy.tv.data.details.mapper.toExtraVideosCachePayload
import su.afk.yummy.tv.data.details.mapper.toThumbnailUrlByNumber
import su.afk.yummy.tv.data.details.network.MalEpisodeFlagsApi
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.data.details.network.YummyEpisodesApi
import su.afk.yummy.tv.data.details.utils.previousTvEntryIds
import su.afk.yummy.tv.data.details.utils.totalOrAiredCount
import su.afk.yummy.tv.data.details.storage.mapper.toAccountUserRatingEntry
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeDetailsCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeRecommendationsCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeRecommendationsWithReactions
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeTrailersCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeVideosCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeWatchProgress
import su.afk.yummy.tv.data.details.utils.isFullyAired
import su.afk.yummy.tv.domain.anime.model.AnimeRelation
import su.afk.yummy.tv.domain.anime.model.AnimeRelationKind
import su.afk.yummy.tv.domain.anime.model.AnimeRelationReference
import su.afk.yummy.tv.domain.anime.model.StudioUrlUnavailableException
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.domain.anime.utils.dropDuplicateExtras
import su.afk.yummy.tv.domain.anime.utils.mergeExtraDubbings
import su.afk.yummy.tv.domain.anime.utils.retainBaseDubbings
import java.util.concurrent.ConcurrentHashMap
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeDetails as toStoredAnimeDetails
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeRecommendations as toStoredAnimeRecommendations
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeTrailers as toStoredAnimeTrailers
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeVideos as toStoredAnimeVideos

private const val ANIME_DETAILS_TTL_MS = 24 * 60 * 60 * 1000L
private const val ANIME_VIDEOS_TTL_MS = 5 * 60 * 1000L
private const val EXTRA_VIDEOS_TTL_MS = 5 * 60 * 1000L
private const val EXTRA_VIDEOS_TIMEOUT_MS = 8_000L
private const val EXTRA_VIDEOS_CACHE_NAMESPACE = "extra-videos"
private const val ANIME_PUBLIC_EXTRAS_TTL_MS = 6 * 60 * 60 * 1000L
private const val ANIME_RELATION_TTL_MS = 24 * 60 * 60 * 1000L
private const val ANIME_PERSONAL_RECOMMENDATIONS_TTL_MS = 5 * 60 * 1000L
private const val ANIME_RECOMMENDATIONS_CACHE_NAMESPACE = "anime-recommendations"
// v2: документ содержит кадры TMDB (previewUrl, stills). v3: у записей, чьи серии в TMDB идут сквозной нумерацией с другой
// записью, сервер больше не отдаёт чужие серии (раньше у 3-го сезона лежали кадры, названия и описания 1-го).
// v4: запрос несёт смещение серий предыдущих сезонов франшизы (?offset=), оно входит в ключ.
private const val ANIME_EPISODE_INFO_CACHE_NAMESPACE = "anime-episode-info-v4"
// Сколько кадров Tenrai (по одному на серию) отдавать для ряда «Скриншоты».
private const val EPISODE_FRAMES_LIMIT = 10
private const val ANIME_EPISODE_INFO_TTL_MS = 7 * 24 * 60 * 60 * 1000L

// v2: документ теперь содержит и картинки серий; кэш с одними пометками их не отдаст.
private const val MAL_EPISODES_CACHE_NAMESPACE = "anime-mal-episodes-v2"
private const val ANIME_EPISODE_FLAGS_FINISHED_TTL_MS = 30 * 24 * 60 * 60 * 1000L
private const val ANIME_EPISODE_FLAGS_ONGOING_TTL_MS = 24 * 60 * 60 * 1000L

class YaniAnimeRepository(
    private val api: YaniAnimeApi,
    private val episodesApi: YummyEpisodesApi,
    private val episodeFlagsApi: MalEpisodeFlagsApi,
    private val animeStorage: AnimeStorage,
    private val accountStorage: AccountStorage,
    private val settingsStore: YaniAccountSettingsStore,
    private val watchProgressStore: WatchProgressStorage,
    private val documentCache: DocumentCacheStorage,
    private val extraVideoRepositories: Set<@JvmSuppressWildcards ExtraVideoRepository> = emptySet(),
) : AnimeRepository {

    private val malEpisodesLocks = ConcurrentHashMap<Int, Mutex>()

    override suspend fun getAnimeDetails(animeId: Int): AnimeDetails = withContext(Dispatchers.IO) {
        val languageCode = settingsStore.currentLanguageCode()
        val stored = animeStorage.getDetails(animeId, languageCode)
        if (stored?.isFresh(ANIME_DETAILS_TTL_MS) == true) {
            return@withContext stored.toStoredAnimeDetails()
        }

        runSuspendCatching {
            fetchDetailsFromNetwork(animeId, languageCode)
        }.getOrElse { error ->
            stored?.toStoredAnimeDetails()
                ?: throw error
        }
    }

    override suspend fun getCachedAnimeDetails(animeId: Int): AnimeDetails? =
        withContext(Dispatchers.IO) {
            animeStorage.getDetails(animeId, settingsStore.currentLanguageCode())
                ?.toStoredAnimeDetails()
        }

    override suspend fun getAnimeVideos(animeId: Int): List<AnimeVideo> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            withExtraVideos(animeId, allowNetwork = true) { loadBaseVideos(animeId, languageCode) }
        }

    override fun observeAnimeVideos(animeId: Int): Flow<List<AnimeVideo>> = flow {
        coroutineScope {
            val languageCode = settingsStore.currentLanguageCode()
            val extras = async { fetchExtraVideos(animeId, allowNetwork = true, forceExtras = false) }
            val base = loadBaseVideos(animeId, languageCode)
            emit(base)
            if (extraVideoRepositories.isNotEmpty()) {
                val merged = mergeExtraVideos(base, extras.await())
                if (merged.size != base.size) emit(merged)
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun loadBaseVideos(animeId: Int, languageCode: String): List<AnimeVideo> =
        offlineFirstCache(
            read = { animeStorage.getVideos(animeId, languageCode) },
            isFresh = { it.isFresh(ANIME_VIDEOS_TTL_MS) },
            toDomain = { it.toStoredAnimeVideos() },
            fetchAndSave = { fetchVideosFromNetwork(animeId, languageCode) },
        )

    override suspend fun refreshAnimeVideos(animeId: Int): List<AnimeVideo> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            withExtraVideos(animeId, allowNetwork = true) {
                offlineFirstCache(
                    forceRefresh = true,
                    read = { animeStorage.getVideos(animeId, languageCode) },
                    isFresh = { it.isFresh(ANIME_VIDEOS_TTL_MS) },
                    toDomain = { it.toStoredAnimeVideos() },
                    fetchAndSave = { fetchVideosFromNetwork(animeId, languageCode) },
                )
            }
        }

    override suspend fun fetchAnimeVideos(animeId: Int): List<AnimeVideo> =
        withContext(Dispatchers.IO) {
            withExtraVideos(animeId, allowNetwork = true, forceExtras = true) {
                fetchVideosFromNetwork(animeId, settingsStore.currentLanguageCode()).toStoredAnimeVideos()
            }
        }

    override suspend fun getCachedAnimeVideos(animeId: Int): List<AnimeVideo>? =
        withContext(Dispatchers.IO) {
            val base = animeStorage.getVideos(animeId, settingsStore.currentLanguageCode())
                ?.toStoredAnimeVideos()
                ?: return@withContext null
            withExtraVideos(animeId, allowNetwork = false) { base }
        }

    /**
     * Видео подключаемых модулей-источников. В кэш Yani не попадают: набор живёт только в памяти.
     * Сбой модуля не ломает основную выдачу. Модули опрашиваются одновременно друг с другом
     * и параллельно с основным запросом Yani; склейка с основной выдачей идёт после обоих.
     */
    private suspend fun withExtraVideos(
        animeId: Int,
        allowNetwork: Boolean,
        forceExtras: Boolean = false,
        loadBase: suspend () -> List<AnimeVideo>,
    ): List<AnimeVideo> {
        if (extraVideoRepositories.isEmpty()) return loadBase()
        val (base, fetched) = coroutineScope {
            val extras = async { fetchExtraVideos(animeId, allowNetwork, forceExtras) }
            val base = loadBase()
            base to extras.await()
        }
        return mergeExtraVideos(base, fetched)
    }

    private fun mergeExtraVideos(
        base: List<AnimeVideo>,
        fetched: List<Pair<ExtraVideoRepository, List<AnimeVideo>>>,
    ): List<AnimeVideo> {
        // Стабильная сортировка: при равном приоритете порядок источников прежний.
        val extras = dropDuplicateExtras(
            fetched.sortedByDescending { (repository, _) -> repository.priority }
                .map { (repository, videos) ->
                    val merged = mergeExtraDubbings(base, videos)
                    if (repository.requiresMatchingDubbing) retainBaseDubbings(base, merged) else merged
                },
        )
        return if (extras.isEmpty()) base else base + extras
    }

    private suspend fun fetchExtraVideos(
        animeId: Int,
        allowNetwork: Boolean,
        forceExtras: Boolean,
    ): List<Pair<ExtraVideoRepository, List<AnimeVideo>>> {
        val details = runSuspendCatching {
            if (allowNetwork) getAnimeDetails(animeId) else getCachedAnimeDetails(animeId)
        }.getOrNull() ?: return emptyList()
        return coroutineScope {
            extraVideoRepositories.map { repository ->
                async {
                    repository to runSuspendCatching {
                        withTimeoutOrNull(EXTRA_VIDEOS_TIMEOUT_MS) {
                            cachedExtraVideos(animeId, repository, details, forceRefresh = forceExtras)
                        }
                    }.getOrNull().orEmpty()
                }
            }.awaitAll()
        }
    }

    /**
     * Видео одного модуля через Room-кэш [EXTRA_VIDEOS_TTL_MS]: свежая запись отдаётся без сети.
     * Ошибка модуля не кэшируется (при ошибке отдаётся просроченная запись, если она есть),
     * поэтому сбой не залипает на время TTL; успешный пустой ответ кэшируется.
     */
    private suspend fun cachedExtraVideos(
        animeId: Int,
        repository: ExtraVideoRepository,
        details: AnimeDetails,
        forceRefresh: Boolean,
    ): List<AnimeVideo> = documentCache.getOrFetchJson<ExtraVideosCachePayload>(
        cacheKey = extraVideosCacheKey(animeId, repository, settingsStore.currentLanguageCode()),
        ttlMs = EXTRA_VIDEOS_TTL_MS,
        forceRefresh = forceRefresh,
    ) {
        repository.videos(details).toExtraVideosCachePayload()
    }.toDomain()

    override suspend fun getAnimeRecommendations(
        animeId: Int,
        fromAi: Boolean,
    ): List<AnimeRecommendation> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            val stored = animeStorage.getRecommendations(animeId, languageCode, fromAi)
            runSuspendCatching {
                val userId = settingsStore.yaniUserId.first().coerceAtLeast(0)
                val response = documentCache.getOrFetchJson<YaniRecommendationsDto>(
                    cacheKey = recommendationCacheKey(
                        userId = userId,
                        language = languageCode,
                        animeId = animeId,
                        fromAi = fromAi,
                    ),
                    ttlMs = if (fromAi) {
                        ANIME_PUBLIC_EXTRAS_TTL_MS
                    } else {
                        ANIME_PERSONAL_RECOMMENDATIONS_TTL_MS
                    },
                    fetch = {
                        api.getAnimeRecommendations(animeId, fromAi).also { dto ->
                            saveRecommendationsContent(
                                animeId = animeId,
                                languageCode = languageCode,
                                fromAi = fromAi,
                                response = dto.response,
                            )
                        }
                    },
                ).response
                response.toAnimeRecommendationsWithReactions(
                    animeId = animeId,
                    language = languageCode,
                    fromAi = fromAi,
                    cachedAt = System.currentTimeMillis(),
                )
            }.getOrElse { error ->
                stored?.toStoredAnimeRecommendations()
                    ?: emptyList()
            }
        }

    override suspend fun setAnimeRecommendationIgnored(animeId: Int, ignored: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val response = if (ignored) {
                api.ignoreAnimeRecommendation(animeId)
            } else {
                api.restoreAnimeRecommendation(animeId)
            }
            response.response.also { updated ->
                if (updated) {
                    invalidateRecommendationCache()
                    // Единая точка правды для всех экранов: главная фильтрует ленту по этому набору.
                    settingsStore.setRecommendationHidden(animeId, ignored)
                }
            }
        }

    override suspend fun voteAnimeRecommendation(
        animeId: Int,
        similarAnimeId: Int,
        vote: AnimeRecommendationVote,
    ): AnimeRecommendationReaction = withContext(Dispatchers.IO) {
        val response = if (vote == AnimeRecommendationVote.NONE) {
            api.deleteAnimeRecommendationVote(animeId, similarAnimeId)
        } else {
            api.voteAnimeRecommendation(animeId, similarAnimeId, vote.apiValue)
        }.response
        invalidateRecommendationCache()
        AnimeRecommendationReaction(
            likes = response.likes,
            dislikes = response.dislikes,
            vote = AnimeRecommendationVote.fromApi(response.vote),
        )
    }

    override suspend fun getAnimeEpisodeInfo(animeId: Int): Map<String, AnimeEpisodeInfo> =
        withContext(Dispatchers.IO) {
            runSuspendCatching {
                loadYummyEpisodes(animeId)?.toAnimeEpisodeInfoByNumber().orEmpty()
            }.getOrElse {
                // Названия серий — необязательная надстройка: экран должен работать и без них.
                emptyMap()
            }
        }

    override suspend fun getAnimeTmdbImages(animeId: Int): AnimeTmdbImages =
        withContext(Dispatchers.IO) {
            runSuspendCatching {
                loadYummyEpisodes(animeId)?.toAnimeTmdbImages() ?: AnimeTmdbImages()
            }.getOrElse {
                // Картинки TMDB — необязательная надстройка: экран работает и со своими.
                AnimeTmdbImages()
            }
        }

    override suspend fun getAnimeEpisodeFrames(animeId: Int): AnimeEpisodeFrames =
        AnimeEpisodeFrames(
            tmdbStillUrls = getAnimeTmdbImages(animeId).firstEpisodeStillUrls,
            tenraiFrameUrls = getAnimeEpisodeThumbnails(animeId).toOrderedFrameUrls(EPISODE_FRAMES_LIMIT),
        )

    /** Один документ YummyTV API на тайтл: из него берутся и названия серий, и картинки TMDB. */
    private suspend fun loadYummyEpisodes(animeId: Int): YummyEpisodesDto? {
        val details = getAnimeDetails(animeId)
        val malId = details.malId ?: return null
        val offset = previousSeasonsEpisodeCount(animeId, details)
        return documentCache.getOrFetchJson<YummyEpisodesDto>(
            cacheKey = "$ANIME_EPISODE_INFO_CACHE_NAMESPACE:$malId:$offset",
            ttlMs = ANIME_EPISODE_INFO_TTL_MS,
            fetch = { episodesApi.getEpisodes(malId, offset) },
        )
    }

    /**
     * Сколько серий в предыдущих TV-записях франшизы. TMDB часто хранит все сезоны одним со сквозной нумерацией
     * (у «Монолога фармацевта» 3-й сезон начинается с 37-й серии), и серверу нужно это смещение, чтобы отдать серии
     * именно этой записи. Если у какой-то записи число серий неизвестно, смещение неточно — берём 0 (как раньше).
     */
    private suspend fun previousSeasonsEpisodeCount(animeId: Int, details: AnimeDetails): Int {
        val previousIds = details.viewingOrder.previousTvEntryIds(animeId)
        if (previousIds.isEmpty()) return 0
        val counts = runSuspendCatching {
            previousIds.map { id -> getAnimeDetails(id).episodes.totalOrAiredCount() }
        }.getOrNull() ?: return 0
        return if (counts.any { it == null }) 0 else counts.sumOf { it ?: 0 }
    }

    override fun observeAnimeEpisodeFlags(animeId: Int): Flow<Map<String, EpisodeFlags>> = channelFlow {
        // Пометки — необязательная надстройка: экран должен работать и без них.
        val episodes = loadMalEpisodes(animeId) { firstPage -> send(firstPage.toEpisodeFlagsByNumber()) }
        send(episodes?.toEpisodeFlagsByNumber().orEmpty())
    }

    override suspend fun getAnimeEpisodeThumbnails(animeId: Int): Map<String, String> =
        // Превью — необязательная надстройка: есть запасной источник.
        loadMalEpisodes(animeId)?.toThumbnailUrlByNumber().orEmpty()

    /**
     * Серии тайтла из MyAnimeList (пометки и картинки в одном документе). Пометки и превью одного тайтла просит один и тот же
     * экран одновременно, поэтому загрузки одного тайтла идут по очереди — второй вызов уже читает кэш; разные тайтлы
     * друг друга не ждут (скорость запросов ограничивает сам API-клиент). [onFirstPage] вызывается, только если этот вызов
     * сам качает длинный список с сети.
     */
    private suspend fun loadMalEpisodes(
        animeId: Int,
        onFirstPage: suspend (MalEpisodesDto) -> Unit = {},
    ): MalEpisodesDto? =
        withContext(Dispatchers.IO) {
            runSuspendCatching {
                val details = getAnimeDetails(animeId)
                val malId = details.malId ?: return@withContext null
                // Статус в ключе: когда тайтл дошёл до последней серии, неполный кэш идущего не залёживается на полгода.
                val finished = details.episodes.isFullyAired()
                malEpisodesLocks.getOrPut(malId) { Mutex() }.withLock {
                    documentCache.getOrFetchJson<MalEpisodesDto>(
                        cacheKey = "$MAL_EPISODES_CACHE_NAMESPACE:$malId:${if (finished) "finished" else "ongoing"}",
                        ttlMs = if (finished) ANIME_EPISODE_FLAGS_FINISHED_TTL_MS else ANIME_EPISODE_FLAGS_ONGOING_TTL_MS,
                        fetch = { episodeFlagsApi.getEpisodes(malId, onFirstPage) },
                    )
                }
            }.getOrNull()
        }

    override suspend fun getAnimeTrailers(animeId: Int): List<AnimeTrailer> =
        withContext(Dispatchers.IO) {
            val languageCode = settingsStore.currentLanguageCode()
            offlineFirstCache(
                read = { animeStorage.getTrailers(animeId, languageCode) },
                isFresh = { it.isFresh(ANIME_PUBLIC_EXTRAS_TTL_MS) },
                toDomain = { it.toStoredAnimeTrailers() },
                fetchAndSave = { fetchTrailersFromNetwork(animeId, languageCode) },
                onMissing = { emptyList() },
            )
        }

    override suspend fun getAnimeRelation(
        reference: AnimeRelationReference,
    ): AnimeRelation = withContext(Dispatchers.IO) {
        val language = settingsStore.currentLanguageCode()
        val referenceKey = when (reference.kind) {
            AnimeRelationKind.STUDIO ->
                reference.url
                    ?.substringBefore('?')
                    ?.trimEnd('/')
                    ?.substringAfterLast('/')
                    ?.takeIf { it.isNotBlank() }
                    ?: throw StudioUrlUnavailableException()

            else -> reference.id.toString()
        }
        documentCache.getOrFetchJson<AnimeRelationCachePayload>(
            cacheKey = "anime-relation:$language:${reference.kind.name}:$referenceKey",
            ttlMs = ANIME_RELATION_TTL_MS,
            fetch = { fetchAnimeRelation(reference, referenceKey) },
        ).toDomain()
    }

    private suspend fun fetchAnimeRelation(
        reference: AnimeRelationReference,
        referenceKey: String,
    ): AnimeRelationCachePayload = when (reference.kind) {
        AnimeRelationKind.STUDIO -> {
            val studio = api.getStudio(referenceKey).response
            val id = studio.id ?: reference.id
            AnimeRelationCachePayload(
                studio = studio,
                anime = api.getRelatedAnime(reference.kind, id).response,
            )
        }

        AnimeRelationKind.DIRECTOR -> {
            val director = api.getDirector(reference.id).response
            AnimeRelationCachePayload(
                director = director,
                anime = api.getRelatedAnime(
                    reference.kind,
                    director.id ?: reference.id,
                ).response,
            )
        }

        AnimeRelationKind.GENRE -> {
            val genre = api.getGenre(reference.id).response
            AnimeRelationCachePayload(
                genre = genre,
                anime = api.getRelatedAnime(
                    reference.kind,
                    genre.id ?: reference.id,
                ).response,
            )
        }
    }

    override fun observeWatchProgress(animeId: Int): Flow<List<AnimeWatchProgress>> =
        watchProgressStore.observeByAnimeId(animeId)
            .map { entries -> entries.map { it.toAnimeWatchProgress() } }
            .distinctUntilChanged()

    private suspend fun fetchDetailsFromNetwork(
        animeId: Int,
        languageCode: String,
    ): AnimeDetails {
        val dto = api.getAnimeDetails(animeId)
        val cachedAt = System.currentTimeMillis()
        // Если удалось сохранить в кэш — возвращаем результат через тот же cache->domain
        // маппер, что и при чтении из кэша, чтобы свежая загрузка не расходилась с ним.
        val result = dto.toAnimeDetailsCache(
            language = languageCode,
            cachedAt = cachedAt,
        )?.let { cache ->
            animeStorage.saveDetails(cache)
            cache.toStoredAnimeDetails()
        } ?: dto.toAnimeDetails()
        saveUserRatingFromDetails(dto, animeId, cachedAt)
        return result
    }

    private suspend fun saveUserRatingFromDetails(
        dto: YaniAnimeDetailsDto,
        animeId: Int,
        cachedAt: Long,
    ) {
        val userId = settingsStore.yaniUserId.first()
        if (userId <= 0 || dto.response.animeId == null) return

        accountStorage.saveUserRating(
            dto.response.user?.rating?.toInt()?.takeIf { it in 1..10 }.toAccountUserRatingEntry(
                userId = userId,
                animeId = animeId,
                cachedAt = cachedAt,
            ),
        )
    }

    private suspend fun fetchVideosFromNetwork(
        animeId: Int,
        languageCode: String,
    ): AnimeVideosCache {
        val dto = api.getAnimeVideos(animeId)
        val cache = dto.response.toAnimeVideosCache(
            animeId = animeId,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        animeStorage.saveVideos(cache)
        return cache
    }

    private suspend fun saveRecommendationsContent(
        animeId: Int,
        languageCode: String,
        fromAi: Boolean,
        response: List<YaniRecommendationItemDto>,
    ) {
        val cache =
            response.toAnimeRecommendationsCache(
                animeId = animeId,
                language = languageCode,
                fromAi = fromAi,
                cachedAt = System.currentTimeMillis(),
            )
        animeStorage.saveRecommendations(cache)
    }

    private suspend fun invalidateRecommendationCache() {
        // Инвалидируем рекомендации только текущего пользователя (по всем anime), а не всех.
        val userId = settingsStore.yaniUserId.first().coerceAtLeast(0)
        documentCache.deleteByPrefix("user:$userId:$ANIME_RECOMMENDATIONS_CACHE_NAMESPACE:")
    }

    private fun extraVideosCacheKey(
        animeId: Int,
        repository: ExtraVideoRepository,
        language: String,
    ): String = "$EXTRA_VIDEOS_CACHE_NAMESPACE:$language:$animeId:${repository.sourceId}"

    private fun recommendationCacheKey(
        userId: Int,
        language: String,
        animeId: Int,
        fromAi: Boolean,
    ): String = recommendationCachePrefix(userId, language, animeId) + fromAi

    private fun recommendationCachePrefix(
        userId: Int,
        language: String,
        animeId: Int,
    ): String = "user:$userId:$ANIME_RECOMMENDATIONS_CACHE_NAMESPACE:$language:$animeId:"

    private suspend fun fetchTrailersFromNetwork(
        animeId: Int,
        languageCode: String,
    ): AnimeTrailersCache {
        val cache = api.getAnimeTrailers(animeId).response.toAnimeTrailersCache(
            animeId = animeId,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        animeStorage.saveTrailers(cache)
        return cache
    }
}
