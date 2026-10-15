package su.afk.yummy.tv.source.anime4k.data.signing

/**
 * Снимает XOR-маску с секрета сборки. Вызывается из сгенерированного `BuildConfig`
 * (см. `buildConfigObfuscatedSecret` в build-logic), поэтому объект публичный.
 */
object SecretDecoder {

    @JvmStatic
    fun decode(data: ByteArray, key: ByteArray): String =
        String(ByteArray(data.size) { (data[it].toInt() xor key[it % key.size].toInt()).toByte() })
}
