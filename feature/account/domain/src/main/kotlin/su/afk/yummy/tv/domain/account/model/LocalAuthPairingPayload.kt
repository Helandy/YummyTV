package su.afk.yummy.tv.domain.account.model

/**
 * Содержимое QR-кода сопряжения, который ТВ показывает рядом с текстовым кодом.
 *
 * IP и порт в QR не кладём: у приставки бывает несколько интерфейсов, а NSD-поиск на телефоне
 * всё равно работает и находит ТВ по имени сервиса.
 *
 * @property deviceId NSD-имя сервиса ТВ (совпадает с [DiscoveredDevice.id]); `null`, если
 * отсканирован голый код без адресата.
 * @property code Нормализованный код сопряжения длиной [LocalAuthCode.LENGTH].
 */
data class LocalAuthPairingPayload(
    val deviceId: String?,
    val code: String,
)
