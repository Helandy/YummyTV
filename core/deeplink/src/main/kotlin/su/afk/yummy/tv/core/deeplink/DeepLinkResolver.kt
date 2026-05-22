package su.afk.yummy.tv.core.deeplink

import android.net.Uri
import androidx.navigation3.runtime.NavKey

interface DeepLinkResolver {
    fun resolve(uri: Uri): NavKey?
}
