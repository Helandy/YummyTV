package su.afk.yummy.tv.core.utils.system

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/** Кладёт [text] в буфер обмена; false, если система буфер не отдала. */
fun Context.copyToClipboard(label: String, text: String): Boolean {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    return clipboard != null && try {
        clipboard.primaryClip = ClipData.newPlainText(label, text)
        true
    } catch (_: RuntimeException) {
        false
    }
}
