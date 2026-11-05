package su.afk.yummy.tv.data.player.extractor.alloha

import java.security.MessageDigest
import java.util.Random

/**
 * What the player page's `Tw()` feeds into its fingerprint hash, in the page's order. The page reads
 * these from the browser; the app has no browser, so [allohaClientProfile] fills them in from the device
 * and from a seed that is generated once per install.
 */
internal data class AllohaDeviceTraits(
    val userAgent: String,
    val timeZone: String,
    /** `max(screen.width, screen.height)` in CSS pixels. */
    val screenLong: Int,
    /** `min(screen.width, screen.height)` in CSS pixels. */
    val screenShort: Int,
    val language: String,
    /** `navigator.deviceMemory`: a power of two up to 8. */
    val deviceMemoryGb: Double,
    val cores: Int,
    /** What `canvas.toDataURL()` returned. */
    val canvas: String,
    /** `UNMASKED_VENDOR_WEBGL|UNMASKED_RENDERER_WEBGL`. */
    val webglRenderer: String,
    /** `OfflineAudioContext(1, 44100, 44100)` renders exactly this many frames. */
    val audioLength: Int = AUDIO_LENGTH,
)

internal const val AUDIO_LENGTH = 44_100

private val VERSION_GROUP = Regex("""\d+(?:[._]\d+)+""")

/**
 * The page's fingerprint: SHA-256 (64 lowercase hex) of the traits joined with `||`. The user agent goes in
 * without its version numbers, as the page does it.
 */
internal fun allohaFingerprint(traits: AllohaDeviceTraits): String {
    val values = listOf(
        traits.userAgent.replace(VERSION_GROUP, ""),
        traits.timeZone,
        "${traits.screenLong}x${traits.screenShort}",
        traits.language,
        jsNumber(traits.deviceMemoryGb),
        traits.cores.toString(),
        traits.canvas,
        traits.webglRenderer,
        traits.audioLength.toString(),
    )
    return MessageDigest.getInstance("SHA-256")
        .digest(values.joinToString("||").toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

/** A number the way JavaScript prints it: `8`, not `8.0`. */
internal fun jsNumber(value: Double): String =
    if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()

private const val BASE64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
private const val CANVAS_PREFIX = "data:image/png;base64,iVBORw0KGgo"
private const val CANVAS_BODY_CHARS = 1_800

/**
 * A canvas image stands in for the real pixels, which differ per GPU and fonts and cannot be drawn without a
 * browser. The page only ever sends the hash, so the image is a deterministic pseudo-random PNG-looking string
 * of the size a small text canvas has, different for every [seed].
 */
internal fun allohaCanvasSample(seed: Long): String {
    val random = Random(seed)
    val body = CharArray(CANVAS_BODY_CHARS) { BASE64_ALPHABET[random.nextInt(BASE64_ALPHABET.length)] }
    return CANVAS_PREFIX + String(body)
}

/** Renderers real Android WebViews report (ANGLE over the GPU of phones and TV boxes). */
private val WEBGL_RENDERERS = listOf(
    "Google Inc. (ARM)|ANGLE (ARM, Mali-G52, OpenGL ES 3.2)",
    "Google Inc. (ARM)|ANGLE (ARM, Mali-G57 MC2, OpenGL ES 3.2)",
    "Google Inc. (ARM)|ANGLE (ARM, Mali-G31, OpenGL ES 3.2)",
    "Google Inc. (ARM)|ANGLE (ARM, Mali-G610, OpenGL ES 3.2)",
    "Google Inc. (Qualcomm)|ANGLE (Qualcomm, Adreno (TM) 610, OpenGL ES 3.2)",
    "Google Inc. (Qualcomm)|ANGLE (Qualcomm, Adreno (TM) 650, OpenGL ES 3.2)",
    "Google Inc. (Qualcomm)|ANGLE (Qualcomm, Adreno (TM) 730, OpenGL ES 3.2)",
    "Google Inc. (Imagination Technologies)|ANGLE (Imagination Technologies, PowerVR Rogue GE8320, OpenGL ES 3.2)",
)

/** The renderer this install reports; stays the same for the same [seed]. */
internal fun allohaWebglRenderer(seed: Long): String =
    WEBGL_RENDERERS[Math.floorMod(seed xor (seed ushr 17), WEBGL_RENDERERS.size.toLong()).toInt()]
