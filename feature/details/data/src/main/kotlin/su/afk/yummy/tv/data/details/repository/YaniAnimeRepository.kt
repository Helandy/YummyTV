package su.afk.yummy.tv.data.details.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeRecommendation
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationReaction
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationVote
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeWatchProgress
import su.afk.yummy.tv.core.network.YaniApiJson
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorageStore
import su.afk.yummy.tv.core.storage.anime.AnimeStorageStore
import su.afk.yummy.tv.core.storage.anime.isFresh
import su.afk.yummy.tv.core.storage.document.DocumentCacheStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.dto.YaniDirectorResponseDto
import su.afk.yummy.tv.data.details.dto.YaniGenreResponseDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationItemDto
import su.afk.yummy.tv.data.details.dto.YaniRecommendationsDto
import su.afk.yummy.tv.data.details.dto.YaniRelatedAnimeDto
import su.afk.yummy.tv.data.details.dto.YaniStudioResponseDto
import su.afk.yummy.tv.data.details.mapper.toAnimeDetails
import su.afk.yummy.tv.data.details.mapper.toAnimeRelation
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.data.details.storage.mapper.toAccountUserRatingEntry
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeDetailsCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeRecommendationsCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeTrailersCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeVideosCache
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeWatchProgress
import su.afk.yummy.tv.domain.anime.model.AnimeRelation
import su.afk.yummy.tv.domain.anime.model.AnimeRelationKind
import su.afk.yummy.tv.domain.anime.model.AnimeRelationReference
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeDetails as toStoredAnimeDetails
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeRecommendations as toStoredAnimeRecommendations
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeTrailers as toStoredAnimeTrailers
import su.afk.yummy.tv.data.details.storage.mapper.toAnimeVideos as toStoredAnimeVideos

private const val ANIME_DETAILS_TTL_MS = 24 * 60 * 60 * 1000L
private const val ANIME_VIDEOS_TTL_MS = 5 * 60 * 1000L
private const val ANIME_PUBLIC_EXTRAS_TTL_MS = 6 * 60 * 60 * 1000L
private const val ANIME_RELATION_TTL_MS = 24 * 60 * 60 * 1000L
private const val ANIME_PERSONAL_RECOMMENDATIONS_TTL_MS = 5 * 60 * 1000L
private const val ANIME_RECOMMENDATIONS_CACHE_NAMESPACE = "anime-recommendations"

class YaniAnimeRepository(
    private val api: YaniAnimeApi,
    private val animeStorage: AnimeStorageStore,
    private val accountStorage: AccountStorageStore,
    private val settingsStore: SettingsStore,
    private val watchProgressStore: WatchProgressStore,
    private val documentCache: DocumentCacheStore,
) : AnimeRepository {

    override suspend fun getAnimeDetails(animeId: Int): AnimeDetails = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first()
        val languageCode = language.apiCode
        val stored = animeStorage.getDetails(animeId, languageCode)
        if (stored?.isFresh(ANIME_DETAILS_TTL_MS) == true) {
            return@withContext stored.toStoredAnimeDetails()
        }

        try {
            fetchDetailsFromNetwork(animeId, languageCode)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            stored?.toStoredAnimeDetails()
                ?: throw error
        }
    }

    override suspend fun getCachedAnimeDetails(animeId: Int): AnimeDetails? =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            animeStorage.getDetails(animeId, language.apiCode)?.toStoredAnimeDetails()
        }

    override suspend fun getAnimeVideos(animeId: Int): List<AnimeVideo> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = animeStorage.getVideos(animeId, languageCode)
            if (stored?.isFresh(ANIME_VIDEOS_TTL_MS) == true) {
                return@withContext stored.toStoredAnimeVideos()
            }

            try {
                fetchVideosFromNetwork(animeId, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toStoredAnimeVideos()
                    ?: throw error
            }
        }

    override suspend fun refreshAnimeVideos(animeId: Int): List<AnimeVideo> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            try {
                fetchVideosFromNetwork(animeId, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                animeStorage.getVideos(animeId, languageCode)?.toStoredAnimeVideos()
                    ?: throw error
            }
        }

    override suspend fun getCachedAnimeVideos(animeId: Int): List<AnimeVideo>? =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            animeStorage.getVideos(animeId, language.apiCode)?.toStoredAnimeVideos()
        }

    override suspend fun getAnimeRecommendations(
        animeId: Int,
        fromAi: Boolean
    ): List<AnimeRecommendation> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = animeStorage.getRecommendations(animeId, languageCode, fromAi)
            try {
                val userId = settingsStore.yaniUserId.first().coerceAtLeast(0)
                val response = documentCache.getOrFetch(
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
                    decode = { YaniApiJson.decodeFromString<YaniRecommendationsDto>(it) },
                    encode = { YaniApiJson.encodeToString(it) },
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
                mapRecommendations(response, animeId, languageCode, fromAi)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
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

    override suspend fun getAnimeTrailers(animeId: Int): List<AnimeTrailer> =
        withContext(Dispatchers.IO) {
            val language = settingsStore.yaniContentLanguage.first()
            val languageCode = language.apiCode
            val stored = animeStorage.getTrailers(animeId, languageCode)
            if (stored?.isFresh(ANIME_PUBLIC_EXTRAS_TTL_MS) == true) {
                return@withContext stored.toStoredAnimeTrailers()
            }

            try {
                fetchTrailersFromNetwork(animeId, languageCode)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                stored?.toStoredAnimeTrailers()
                    ?: emptyList()
            }
        }

    override suspend fun getAnimeRelation(
        reference: AnimeRelationReference,
    ): AnimeRelation = withContext(Dispatchers.IO) {
        val language = settingsStore.yaniContentLanguage.first().apiCode
        val referenceKey = when (reference.kind) {
            AnimeRelationKind.STUDIO -> reference.url
                ?.substringBefore('?')
                ?.trimEnd('/')
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: error("Studio URL is unavailable")

            else -> reference.id.toString()
        }
        documentCache.getOrFetch(
            cacheKey = "anime-relation:$language:${reference.kind.name}:$referenceKey",
            ttlMs = ANIME_RELATION_TTL_MS,
            decode = { YaniApiJson.decodeFromString<AnimeRelationCachePayload>(it) },
            encode = { YaniApiJson.encodeToString(it) },
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
            )
        )
    }

    private suspend fun fetchVideosFromNetwork(
        animeId: Int,
        languageCode: String,
    ): List<AnimeVideo> {
        val dto = api.getAnimeVideos(animeId)
        val cache = dto.response.toAnimeVideosCache(
            animeId = animeId,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        animeStorage.saveVideos(cache)
        return cache.toStoredAnimeVideos()
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

    private fun mapRecommendations(
        response: List<YaniRecommendationItemDto>,
        animeId: Int,
        languageCode: String,
        fromAi: Boolean,
    ): List<AnimeRecommendation> {
        val cache = response.toAnimeRecommendationsCache(
            animeId = animeId,
            language = languageCode,
            fromAi = fromAi,
            cachedAt = System.currentTimeMillis(),
        )
        val reactionsByAnimeId = response.mapNotNull { item ->
            item.animeId?.let { id -> id to item.likes }
        }.toMap()
        return cache.toStoredAnimeRecommendations().map { recommendation ->
            val reaction = reactionsByAnimeId[recommendation.animeId]
            if (reaction == null) recommendation else recommendation.copy(
                likes = reaction.likes,
                dislikes = reaction.dislikes,
                vote = AnimeRecommendationVote.fromApi(reaction.vote),
            )
        }
    }

    private suspend fun invalidateRecommendationCache() {
        documentCache.deleteUserNamespace(ANIME_RECOMMENDATIONS_CACHE_NAMESPACE)
    }

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
    ): List<AnimeTrailer> {
        val cache = api.getAnimeTrailers(animeId).response.toAnimeTrailersCache(
            animeId = animeId,
            language = languageCode,
            cachedAt = System.currentTimeMillis(),
        )
        animeStorage.saveTrailers(cache)
        return cache.toStoredAnimeTrailers()
    }
}

@Serializable
private data class AnimeRelationCachePayload(
    val studio: YaniStudioResponseDto? = null,
    val director: YaniDirectorResponseDto? = null,
    val genre: YaniGenreResponseDto? = null,
    val anime: List<YaniRelatedAnimeDto> = emptyList(),
) {
    fun toDomain(): AnimeRelation = when {
        studio != null -> studio.toAnimeRelation(anime)
        director != null -> director.toAnimeRelation(anime)
        genre != null -> genre.toAnimeRelation(anime)
        else -> error("Cached anime relation has no metadata")
    }
}
