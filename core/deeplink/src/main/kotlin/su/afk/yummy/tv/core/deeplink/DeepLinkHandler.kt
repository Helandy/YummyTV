package su.afk.yummy.tv.core.deeplink

import android.content.Intent
import su.afk.yummy.tv.core.navigation.NavigationManager
import javax.inject.Inject

interface DeepLinkHandler {
    fun handle(intent: Intent)
}

internal class DeepLinkHandlerImpl @Inject constructor(
    private val resolver: DeepLinkResolver,
    private val navManager: NavigationManager,
) : DeepLinkHandler {

    override fun handle(intent: Intent) {
        val uri = intent.data ?: return
        resolver.resolve(uri)?.let(navManager::navigate)
    }
}
