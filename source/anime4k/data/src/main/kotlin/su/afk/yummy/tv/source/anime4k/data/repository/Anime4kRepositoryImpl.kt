package su.afk.yummy.tv.source.anime4k.data.repository

import su.afk.yummy.tv.source.anime4k.data.dto.Anime4kStreamsRequestDto
import su.afk.yummy.tv.source.anime4k.data.mapper.Anime4kMapper
import su.afk.yummy.tv.source.anime4k.data.network.Anime4kApi
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreams
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTicket
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTitle
import su.afk.yummy.tv.source.anime4k.domain.repository.Anime4kRepository
import su.afk.yummy.tv.core.utils.system.DeviceHashProvider
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class Anime4kRepositoryImpl @Inject constructor(
    private val api: Anime4kApi,
    private val deviceHashProvider: DeviceHashProvider,
) : Anime4kRepository {

    private data class CachedTitle(val title: Anime4kTitle?, val fetchedAtMs: Long)

    private val titleCache = ConcurrentHashMap<Int, CachedTitle>()

    override suspend fun getTitle(malId: Int): Anime4kTitle? {
        val now = System.currentTimeMillis()
        titleCache[malId]?.takeIf { now - it.fetchedAtMs < TITLE_TTL_MS }?.let { return it.title }

        // Сбой сети в кэш не попадает: исключение уйдёт наверх, источник просто не появится в этот раз.
        val title = api.getTitle(malId)?.let { Anime4kMapper.toTitle(malId, it) }
        titleCache[malId] = CachedTitle(title, now)
        return title
    }

    override suspend fun requestTicket(): Anime4kTicket =
        Anime4kMapper.toTicket(api.requestTicket(deviceHashProvider.get()))

    override suspend fun getStreams(ticket: Anime4kTicket, target: Anime4kTarget): Anime4kStreams {
        val request = Anime4kStreamsRequestDto(
            ticket = ticket.value,
            malId = target.malId,
            season = target.season,
            episode = target.episode,
            team = target.team,
        )
        return Anime4kMapper.toStreams(api.getStreams(request, deviceHashProvider.get()))
    }

    private companion object {
        const val TITLE_TTL_MS = 10 * 60 * 1000L
    }
}
