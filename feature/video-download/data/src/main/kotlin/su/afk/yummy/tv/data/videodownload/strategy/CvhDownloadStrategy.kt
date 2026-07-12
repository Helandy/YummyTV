package su.afk.yummy.tv.data.videodownload.strategy

import su.afk.yummy.tv.data.videodownload.worker.utils.withDownloadRequestHeaders
import javax.inject.Inject

/**
 * CVH's CDN (okcdn.ru) rejects requests carrying a foreign Referer/Origin with a 400; its own live
 * playback already works with no Referer/Origin, only User-Agent. Everything else is generic.
 */
internal class CvhDownloadStrategy @Inject constructor() :
    DownloadPlayerStrategy by DefaultDownloadStrategy {
    override val playerLabel: String = "Cvh"

    override fun decorateHeaders(
        headers: Map<String, String>,
        iframeUrl: String
    ): Map<String, String> =
        headers.withDownloadRequestHeaders(iframeUrl, skipRefererOrigin = true)
}
