package su.afk.yummy.tv.feature.home.deeplink

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.core.deeplink.api.DeepLinkReference
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.home.navigator.ContinueWatchingLaunchDestination
import javax.inject.Inject

/**
 * Разбирает `yummytv://continue/{animeId}` (ярлыки, виджет) в запуск «продолжить просмотр» и
 * `yummytv://play/{animeId}?episode=&balancer=&dubbing=&position=` в запуск с явным выбором серии,
 * балансера, озвучки и позиции в секундах; все параметры необязательные.
 */
class ContinueWatchingDeepLinkResolver @Inject constructor() : DeepLinkResolver {

    override fun resolve(link: DeepLinkReference): NavKey? {
        val host = link.appLinkHost
        if (host != HOST_CONTINUE && host != HOST_PLAY) return null
        val animeId = link.uri.lastPathSegment?.toIntOrNull() ?: return null
        if (host == HOST_CONTINUE) return ContinueWatchingLaunchDestination(animeId)
        return ContinueWatchingLaunchDestination(
            animeId = animeId,
            episode = link.uri.getQueryParameter(PARAM_EPISODE)?.takeIf(String::isNotBlank),
            player = link.uri.getQueryParameter(PARAM_BALANCER)?.takeIf(String::isNotBlank),
            dubbing = link.uri.getQueryParameter(PARAM_DUBBING)?.takeIf(String::isNotBlank),
            positionSeconds = link.uri.getQueryParameter(PARAM_POSITION)?.toLongOrNull(),
        )
    }

    private companion object {
        const val HOST_CONTINUE = "continue"
        const val HOST_PLAY = "play"
        const val PARAM_EPISODE = "episode"
        const val PARAM_BALANCER = "balancer"
        const val PARAM_DUBBING = "dubbing"
        const val PARAM_POSITION = "position"
    }
}
