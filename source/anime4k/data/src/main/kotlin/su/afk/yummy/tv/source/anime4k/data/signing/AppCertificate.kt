package su.afk.yummy.tv.source.anime4k.data.signing

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** SHA-256 сертификата, которым подписан установленный APK. Перепакованный чужой подписью APK даёт другой отпечаток. */
internal object AppCertificate {

    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    fun sha256Hex(context: Context): String? = runCatching {
        val packageManager = context.packageManager
        val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners?.firstOrNull()
        } else {
            packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                .signatures?.firstOrNull()
        } ?: return null
        MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
    }.getOrNull()

    fun versionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty().ifBlank { "unknown" }
}
