package su.afk.yummy.tv.core.preferences.auth

import kotlinx.coroutines.delay

/** Плоское хранилище записи токена — абстракция над SharedPreferences ради тестов. */
internal interface TokenRecordStore {
    fun readRecord(): String?

    fun writeRecord(record: String)

    fun removeRecord()

    fun readMode(): TokenStorageMode?

    fun writeMode(mode: TokenStorageMode)
}

/**
 * Чтение и запись refresh-токена, устойчивые к нерабочему AndroidKeyStore.
 *
 * Запись помечена режимом, которым она зашифрована, поэтому нечитаемая запись keystore-режима не
 * принимается за битые данные после понижения до [TokenStorageMode.FALLBACK]. Токен удаляется
 * только тогда, когда шифр заведомо исправен (`selfTest`), а конкретная запись всё равно не
 * расшифровывается — раньше любая ошибка keystore молча стирала сессию.
 */
internal class AuthTokenStorage(
    private val store: TokenRecordStore,
    private val cipherFactory: (TokenStorageMode) -> TokenCipher,
    private val onStorageFailure: (String, Throwable?) -> Unit,
    private val onModeChanged: (TokenStorageMode) -> Unit = {},
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) {
    private val ciphers = mutableMapOf<TokenStorageMode, TokenCipher>()

    var mode: TokenStorageMode = store.readMode() ?: TokenStorageMode.KEYSTORE
        private set

    suspend fun read(): String {
        val record = store.readRecord().orEmpty()
        if (record.isBlank()) return ""
        val recordMode = record.recordMode()
        val payload = record.recordPayload()

        val cipher = cipher(recordMode)
        RETRY_DELAYS_MS.forEach { delayMs ->
            // Keystore2 на холодном старте может быть ещё не поднят — это не повод терять сессию.
            if (delayMs > 0) sleep(delayMs)
            runCatching { cipher.decrypt(payload) }.getOrNull()?.let { return it }
        }

        if (runCatching { cipher.selfTest() }.getOrDefault(false)) {
            // Шифр исправен, а запись всё равно не читается — данные действительно битые.
            onStorageFailure("Auth token record is unreadable with a healthy cipher", null)
            store.removeRecord()
            return ""
        }

        onStorageFailure("Auth token cipher is unavailable on read (${recordMode.name})", null)
        if (recordMode == TokenStorageMode.KEYSTORE) switchToFallback()
        return ""
    }

    fun write(token: String) {
        val trimmed = token.trim()
        if (trimmed.isBlank()) {
            store.removeRecord()
            return
        }
        val record = encode(mode, trimmed) ?: run {
            // Keystore отказал на записи — иначе успешный вход превратился бы в ошибку.
            if (mode == TokenStorageMode.KEYSTORE) switchToFallback()
            encode(mode, trimmed)
        }
        if (record == null) {
            onStorageFailure("Auth token could not be stored in any mode", null)
            return
        }
        store.writeRecord(record)
    }

    fun clear() = store.removeRecord()

    private fun encode(mode: TokenStorageMode, token: String): String? =
        runCatching { mode.tag() + cipher(mode).encrypt(token) }
            .onFailure { onStorageFailure("Auth token encryption failed (${mode.name})", it) }
            .getOrNull()

    private fun switchToFallback() {
        if (mode == TokenStorageMode.FALLBACK) return
        mode = TokenStorageMode.FALLBACK
        store.writeMode(TokenStorageMode.FALLBACK)
        onStorageFailure("Auth token storage degraded to fallback", null)
        onModeChanged(TokenStorageMode.FALLBACK)
    }

    private fun cipher(mode: TokenStorageMode): TokenCipher =
        ciphers.getOrPut(mode) { cipherFactory(mode) }

    private fun String.recordMode(): TokenStorageMode = when (first()) {
        FALLBACK_TAG -> TokenStorageMode.FALLBACK
        // Записи до появления меток всегда keystore-режима.
        else -> TokenStorageMode.KEYSTORE
    }

    private fun String.recordPayload(): String = when (first()) {
        FALLBACK_TAG, KEYSTORE_TAG -> substring(1)
        else -> this
    }

    private fun TokenStorageMode.tag(): Char =
        if (this == TokenStorageMode.FALLBACK) FALLBACK_TAG else KEYSTORE_TAG

    private companion object {
        // Base64 NO_WRAP не содержит '*' и '~', поэтому метка не сливается со старыми записями.
        const val KEYSTORE_TAG = '*'
        const val FALLBACK_TAG = '~'
        val RETRY_DELAYS_MS = longArrayOf(0L, 300L, 1_000L)
    }
}
