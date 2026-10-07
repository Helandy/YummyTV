package su.afk.yummy.tv.core.utils.system

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** Кладёт [text] в буфер обмена; false, если система буфер не отдала. */
// UsePropertyAccessSyntax: на compileSdk 37 синтетическое свойство primaryClip доступно только на чтение.
@Suppress("UsePropertyAccessSyntax")
fun Context.copyToClipboard(label: String, text: String): Boolean {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    return clipboard != null && try {
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        true
    } catch (_: RuntimeException) {
        false
    }
}
