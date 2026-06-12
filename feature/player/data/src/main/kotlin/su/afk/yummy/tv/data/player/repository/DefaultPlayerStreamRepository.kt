package su.afk.yummy.tv.data.player.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.data.player.extractor.AksorExtractor
import su.afk.yummy.tv.data.player.extractor.AksorResult
import su.afk.yummy.tv.data.player.extractor.AllohaExtractor
import su.afk.yummy.tv.data.player.extractor.AllohaResult
import su.afk.yummy.tv.data.player.extractor.CvhExtractor
import su.afk.yummy.tv.data.player.extractor.KodikExtractor
import su.afk.yummy.tv.data.player.extractor.KodikResult
import su.afk.yummy.tv.data.player.extractor.RutubeExtractor
import su.afk.yummy.tv.data.player.extractor.RutubeResult
import su.afk.yummy.tv.data.player.extractor.VkExtractor
import su.afk.yummy.tv.data.player.extractor.VkResult
import su.afk.yummy.tv.data.player.utils.BROWSER_STREAM_HEADERS
import su.afk.yummy.tv.domain.player.isAksorPlayerUrl
import su.afk.yummy.tv.domain.player.isAllohaPlayerUrl
import su.afk.yummy.tv.domain.player.isCvhPlayerUrl
import su.afk.yummy.tv.domain.player.isKodikPlayerUrl
import su.afk.yummy.tv.domain.player.isRutubePlayerUrl
import su.afk.yummy.tv.domain.player.isVkPlayerUrl
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import javax.inject.Inject

class DefaultPlayerStreamRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PlayerStreamRepository {

    override suspend fun resolve(request: PlayerStreamRequest): PlayerStreamResolveResult {
        val url = request.iframeUrl
        return when {
            url.isAllohaPlayerUrl() -> AllohaExtractor.extract(url, context)?.toStream()
                ?: PlayerStreamResolveResult.Failed

            url.isKodikPlayerUrl() -> when (val result = KodikExtractor.extract(url)) {
                is KodikResult.Stream -> result.toStream()
                is KodikResult.Blocked -> PlayerStreamResolveResult.KodikBlocked(
                    message = result.message,
                    statusCode = result.statusCode,
                )

                KodikResult.Failed -> PlayerStreamResolveResult.Failed
            }

            url.isAksorPlayerUrl() -> AksorExtractor.extract(url)?.toStream()
                ?: PlayerStreamResolveResult.Failed

            url.isCvhPlayerUrl() -> CvhExtractor.extract(url, request.autoQualityLabel)
                ?.let { qualities ->
                    PlayerStreamResolveResult.Stream(
                        url = qualities.values.last(),
                        headers = BROWSER_STREAM_HEADERS,
                        qualities = qualities,
                    )
                } ?: PlayerStreamResolveResult.Failed

            url.isVkPlayerUrl() -> VkExtractor.extract(
                iframeUrl = url,
                autoQualityLabel = request.autoQualityLabel,
            )?.toStream() ?: PlayerStreamResolveResult.Failed

            url.isRutubePlayerUrl() -> RutubeExtractor.extract(
                iframeUrl = url,
                autoQualityLabel = request.autoQualityLabel,
            )?.toStream() ?: PlayerStreamResolveResult.Failed

            else -> PlayerStreamResolveResult.Unsupported
        }
    }

    private fun AllohaResult.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
        )

    private fun KodikResult.Stream.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
        )

    private fun AksorResult.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
        )

    private fun VkResult.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
        )

    private fun RutubeResult.toStream(): PlayerStreamResolveResult.Stream =
        PlayerStreamResolveResult.Stream(
            url = url,
            headers = headers,
            qualities = qualities,
        )
}
