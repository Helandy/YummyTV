package su.afk.yummy.tv.core.preferences.auth

/**
 * Каким шифром лежит refresh-токен. На кастомных прошивках (SlimBox и прочие сборки для
 * ТВ-боксов) AndroidKeyStore бывает нерабочим — тогда приложение сознательно понижается до
 * [FALLBACK], иначе вход невозможен вовсе.
 */
enum class TokenStorageMode {
    KEYSTORE,
    FALLBACK,
}
