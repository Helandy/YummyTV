package su.afk.yummy.tv.source.aniboom.data.repository

import su.afk.yummy.tv.source.aniboom.data.network.AniboomApi
import su.afk.yummy.tv.source.aniboom.data.utils.ANIBOOM_REFERER
import su.afk.yummy.tv.source.aniboom.data.utils.aniboomEmbedUrl
import su.afk.yummy.tv.source.aniboom.data.utils.aniboomQualities
import su.afk.yummy.tv.source.aniboom.data.utils.parseAniboomHlsUrl
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomStreams
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import su.afk.yummy.tv.source.aniboom.domain.repository.AniboomRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
internal class AniboomRepositoryImpl @Inject constructor(
    private val api: AniboomApi,
) : AniboomRepository {

    override suspend fun getStreams(target: AniboomTarget): AniboomStreams? {
        val html = api.getEmbed(aniboomEmbedUrl(target.aniboomId, target.episode, target.translation), ANIBOOM_REFERER)
            ?: return null
        val hls = parseAniboomHlsUrl(html) ?: throw IOException("Aniboom embed: нет ссылки на HLS")
        // Без списка качеств плеер оставляет одно «Авто», поэтому сбой плейлиста не фатален.
        val qualities = try {
            aniboomQualities(api.getPlaylist(hls), hls)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            emptyMap()
        }
        return AniboomStreams(hls, qualities)
    }
}
