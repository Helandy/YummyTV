package su.afk.yummy.tv.source.ailiberty.data.repository

import su.afk.yummy.tv.source.ailiberty.data.network.AiLibertyApi
import su.afk.yummy.tv.source.ailiberty.data.utils.aiLibertyEpisodeQualities
import su.afk.yummy.tv.source.ailiberty.data.utils.aiLibertyReleaseUrl
import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyStreams
import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyTarget
import su.afk.yummy.tv.source.ailiberty.domain.repository.AiLibertyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AiLibertyRepositoryImpl @Inject constructor(
    private val api: AiLibertyApi,
) : AiLibertyRepository {

    override suspend fun getStreams(target: AiLibertyTarget): AiLibertyStreams? {
        val html = api.getReleasePage(aiLibertyReleaseUrl(target.slug)) ?: return null
        return aiLibertyEpisodeQualities(html, target.episode)?.let(::AiLibertyStreams)
    }
}
