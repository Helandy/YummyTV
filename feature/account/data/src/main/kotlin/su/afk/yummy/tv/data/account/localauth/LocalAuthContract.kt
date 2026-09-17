package su.afk.yummy.tv.data.account.localauth

import android.os.Build

/** Общие для ТВ и телефона детали протокола сопряжения. */
internal object LocalAuthContract {
    const val SERVICE_TYPE = "_yummytv_auth._tcp."
    const val TRANSFER_PATH = "/transfer"

    private const val SERVICE_NAME_PREFIX = "YummyTv"
    private const val MAX_SERVICE_NAME_BYTES = 63

    /**
     * Имя NSD-сервиса ограничено 63 байтами и не должно содержать управляющих символов,
     * а [Build.MODEL] у приставок бывает длинным и с юникодом.
     */
    fun deviceServiceName(model: String? = Build.MODEL): String {
        val sanitized = model.orEmpty()
            .filter { it.isLetterOrDigit() || it == ' ' || it == '-' }
            .trim()
        val name = if (sanitized.isBlank()) SERVICE_NAME_PREFIX else "$SERVICE_NAME_PREFIX-$sanitized"
        return name.encodeToByteArray()
            .take(MAX_SERVICE_NAME_BYTES)
            .toByteArray()
            .decodeToString()
    }

    /** Android нормализует концевые точки в типе сервиса по-разному. */
    fun matchesServiceType(serviceType: String?): Boolean =
        serviceType?.trim('.')?.equals(SERVICE_TYPE.trim('.'), ignoreCase = true) == true
}
