package su.afk.yummy.tv.feature.videodownload.deeplink

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.deeplink.api.DeepLinkReference
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator
import javax.inject.Inject

/** Разбирает `yummytv://downloads` в экран загрузок. */
class VideoDownloadDeepLinkResolver @Inject constructor(
    private val videoDownloadNavigator: IVideoDownloadNavigator,
) : DeepLinkResolver {

    override fun resolve(link: DeepLinkReference): NavKey? =
        if (link.appLinkHost == "downloads") videoDownloadNavigator.getVideoDownloadDest() else null
}
