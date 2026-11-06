package su.afk.yummy.tv.feature.player.utils

// Декодер устройства не справился с форматом/разрешением: тот же поток упадёт так же,
// помогает только другое качество.
private val DECODER_ERROR_CODES = setOf(
    "ERROR_CODE_DECODER_INIT_FAILED",
    "ERROR_CODE_DECODER_QUERY_FAILED",
    "ERROR_CODE_DECODING_FAILED",
    "ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES",
    "ERROR_CODE_DECODING_FORMAT_UNSUPPORTED",
)

/** Имя кода ошибки плеера относится к декодеру устройства (MediaCodecVideoRenderer и др.). */
internal fun String?.isDecoderErrorCode(): Boolean = this in DECODER_ERROR_CODES
