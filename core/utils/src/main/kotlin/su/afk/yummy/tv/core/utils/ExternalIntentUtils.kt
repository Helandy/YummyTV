package su.afk.yummy.tv.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

private const val EXTERNAL_INTENT_TAG = "ExternalIntentUtils"

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

private fun Context.startActivitySafely(intent: Intent): Boolean =
    try {
        startActivity(intent)
        true
    } catch (e: RuntimeException) {
        Log.w(EXTERNAL_INTENT_TAG, "Unable to start external intent", e)
        false
    }
