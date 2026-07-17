package su.afk.yummy.tv.android.lifecycle

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceHashProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun get(): String? {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID,
        )?.trim().orEmpty()
        if (androidId.isBlank()) return null

        val input = "${context.packageName}:$androidId".encodeToByteArray()
        return MessageDigest.getInstance(HASH_ALGORITHM)
            .digest(input)
            .toLowercaseHex()
    }

    private fun ByteArray.toLowercaseHex(): String = buildString(size * 2) {
        for (byte in this@toLowercaseHex) {
            val value = byte.toInt() and 0xff
            append(HEX_DIGITS[value ushr 4])
            append(HEX_DIGITS[value and 0x0f])
        }
    }

    private companion object {
        const val HASH_ALGORITHM = "SHA-256"
        const val HEX_DIGITS = "0123456789abcdef"
    }
}
