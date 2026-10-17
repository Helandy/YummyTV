package su.afk.yummy.tv.core.utils.system

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.openExternalUri(uri: String): Boolean {
    val trimmedUri = uri.trim()
    if (trimmedUri.isEmpty()) return false

    val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(trimmedUri))
        .addCategory(Intent.CATEGORY_BROWSABLE)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    return startActivitySafely(viewIntent) ||
        startActivitySafely(
            Intent.createChooser(viewIntent, null)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
}

/** Системный шаринг текста ([text], например ссылки) через chooser; false — некому отправить. */
fun Context.shareText(text: String, subject: String? = null): Boolean {
    val sendIntent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)
        .apply { subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) } }
    return startActivitySafely(
        Intent.createChooser(sendIntent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

private fun Context.startActivitySafely(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (_: RuntimeException) {
        false
    }
