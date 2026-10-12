package su.afk.yummy.tv.feature.player.common.deeplink

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.qualifiers.ApplicationContext
import su.afk.yummy.tv.core.deeplink.api.DeepLinkReference
import su.afk.yummy.tv.core.deeplink.api.DeepLinkResolver
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import javax.inject.Inject

/**
 * Внешнее открытие локального видеофайла (ACTION_VIEW + `content://`/`file://`): такие Intent
 * приходят из файловых менеджеров и галереи через «Открыть с помощью».
 */
internal class LocalVideoDeepLinkResolver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val playerNavigator: IPlayerNavigator,
) : DeepLinkResolver {

    override fun resolve(link: DeepLinkReference): NavKey? {
        if (!link.isLocalVideo()) return null
        return playerNavigator.getLocalFilePlayerDest(
            uri = link.uri.toString(),
            title = link.uri.displayName(),
        )
    }

    private fun DeepLinkReference.isLocalVideo(): Boolean =
        intent.action == Intent.ACTION_VIEW &&
                (intent.type?.startsWith("video/") == true ||
                        uri.scheme == ContentResolver.SCHEME_CONTENT ||
                        uri.scheme == ContentResolver.SCHEME_FILE)

    /**
     * Читаемое имя файла для заголовка плеера: DISPLAY_NAME для content://,
     * последний сегмент пути для file://. Сбой запроса не должен ломать
     * воспроизведение, поэтому оборачиваем в runCatching и на ошибке отдаём "".
     */
    private fun Uri.displayName(): String {
        val raw = when (scheme) {
            ContentResolver.SCHEME_CONTENT -> runCatching {
                context.contentResolver.query(
                    this,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                }
            }.getOrNull()

            else -> lastPathSegment
        }
        return raw?.substringBeforeLast('.').orEmpty()
    }
}
