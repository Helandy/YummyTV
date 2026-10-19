package su.afk.yummy.tv.core.featuretoggle.api

object FeatureFlags {
    val all: Set<FeatureFlag<*>> = emptySet()

    val minSupportedAppVersion = FeatureFlag.StringFlag(
        key = "app_min_supported_version",
        defaultValue = "",
    )

    /** Версия/идентификатор объявления. Смена значения снова показывает диалог всем. Пустой = выключено. */
    val announcementId = FeatureFlag.StringFlag(
        key = "announcement_id",
        defaultValue = "",
    )

    /** Заголовок объявления. Пустой = заголовок скрыт. */
    val announcementTitle = FeatureFlag.StringFlag(
        key = "announcement_title",
        defaultValue = "",
    )

    /** Текст объявления. Пустой = диалог не показывается. */
    val announcementMessage = FeatureFlag.StringFlag(
        key = "announcement_message",
        defaultValue = "",
    )

    /** Текст кнопки объявления. Пустой = локальный фолбэк. */
    val announcementButton = FeatureFlag.StringFlag(
        key = "announcement_button",
        defaultValue = "",
    )

    /** Адрес API сайта (https). Пустой или совпадающий со встроенным = остаётся встроенный. */
    val siteApiBaseUrl = FeatureFlag.StringFlag(
        key = "site_api_base_url",
        defaultValue = "",
    )

    /** Токен подписи запросов к сайту. Пустой или совпадающий со встроенным = остаётся встроенный. */
    val siteApiToken = FeatureFlag.StringFlag(
        key = "site_api_token",
        defaultValue = "",
    )

    /** Адрес API yani (https). Пустой или совпадающий со встроенным = остаётся встроенный. Подменяются хост и порт. */
    val yaniApiBaseUrl = FeatureFlag.StringFlag(
        key = "yani_api_base_url",
        defaultValue = "",
    )

    /** Доп. строка-сообщение на главном экране (не диалог). Пустой = не показывается. */
    val statusMessage = FeatureFlag.StringFlag(
        key = "status_message_text",
        defaultValue = "",
    )

    /** Резервная прямая ссылка на APK (https), если GitHub Releases недоступен. */
    val updateFallbackApkUrl = FeatureFlag.StringFlag(
        key = "update_fallback_apk_url",
        defaultValue = "",
    )

    /** Версия резервного APK (сравнивается с текущей как обычный релиз). */
    val updateFallbackVersion = FeatureFlag.StringFlag(
        key = "update_fallback_version",
        defaultValue = "",
    )

    /** Описание изменений резервного APK. */
    val updateFallbackChangelog = FeatureFlag.StringFlag(
        key = "update_fallback_changelog",
        defaultValue = "",
    )
}
