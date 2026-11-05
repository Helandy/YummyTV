package su.afk.yummy.tv.data.player.extractor.alloha

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import java.security.SecureRandom
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Who this install looks like to Alloha: the fingerprint for `Borth` and the same screen/platform numbers for the
 * `/events` and `/stat` reports, so the three tell one story. Generated once per install and reused: the site's own
 * player keeps one fingerprint per browser, and a client that changes it on every launch looks like a bot farm.
 */
internal data class AllohaClientProfile(
    val fingerprint: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val windowWidth: Int,
    val windowHeight: Int,
    val pixelRatio: Double,
    val platform: String,
)

private const val PREFS_NAME = "alloha_client"
private const val SEED_KEY = "seed"
private const val LANGUAGE = "ru-RU"
private const val MAX_DEVICE_MEMORY_GB = 8.0
private const val MIN_DEVICE_MEMORY_GB = 0.25
private const val BYTES_PER_GB = 1_073_741_824.0
private const val PLAYER_ASPECT_HEIGHT = 9
private const val PLAYER_ASPECT_WIDTH = 16

/** Builds the profile from the device. Blocking (reads preferences); call from an IO dispatcher. */
internal fun allohaClientProfile(context: Context, userAgent: String): AllohaClientProfile {
    val seed = installSeed(context)
    val metrics = context.resources.displayMetrics
    val density = metrics.density.toDouble().takeIf { it > 0.0 } ?: 1.0
    val cssWidth = (metrics.widthPixels / density).roundToInt()
    val cssHeight = (metrics.heightPixels / density).roundToInt()
    val longSide = max(cssWidth, cssHeight)
    val shortSide = min(cssWidth, cssHeight)
    val traits = AllohaDeviceTraits(
        userAgent = userAgent,
        timeZone = TimeZone.getDefault().id,
        screenLong = longSide,
        screenShort = shortSide,
        language = LANGUAGE,
        deviceMemoryGb = deviceMemoryGb(context),
        cores = Runtime.getRuntime().availableProcessors(),
        canvas = allohaCanvasSample(seed),
        webglRenderer = allohaWebglRenderer(seed),
    )
    return AllohaClientProfile(
        fingerprint = allohaFingerprint(traits),
        screenWidth = cssWidth,
        screenHeight = cssHeight,
        windowWidth = cssWidth,
        windowHeight = cssWidth * PLAYER_ASPECT_HEIGHT / PLAYER_ASPECT_WIDTH,
        pixelRatio = density,
        platform = navigatorPlatform(),
    )
}

/** One random number per install, kept so the fingerprint survives restarts. */
private fun installSeed(context: Context): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.contains(SEED_KEY)) return prefs.getLong(SEED_KEY, 0L)
    return SecureRandom().nextLong().also { prefs.edit().putLong(SEED_KEY, it).apply() }
}

/** `navigator.deviceMemory`: the installed RAM rounded down to a power of two, at most 8. */
private fun deviceMemoryGb(context: Context): Double {
    val info = ActivityManager.MemoryInfo()
    (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(info)
    val gb = info.totalMem / BYTES_PER_GB
    var bucket = MIN_DEVICE_MEMORY_GB
    while (bucket * 2 <= gb && bucket < MAX_DEVICE_MEMORY_GB) bucket *= 2
    return bucket
}

/** `navigator.platform` as Chrome on Android reports it. */
private fun navigatorPlatform(): String = when (Build.SUPPORTED_ABIS.firstOrNull()) {
    "arm64-v8a" -> "Linux armv81"
    "armeabi-v7a" -> "Linux armv7l"
    "x86_64" -> "Linux x86_64"
    "x86" -> "Linux i686"
    else -> "Linux armv81"
}
