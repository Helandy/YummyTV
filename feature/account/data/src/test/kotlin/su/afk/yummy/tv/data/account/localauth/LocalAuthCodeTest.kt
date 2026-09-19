package su.afk.yummy.tv.data.account.localauth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.data.account.utils.LocalAuthCrypto
import su.afk.yummy.tv.domain.account.model.LocalAuthCode

/**
 * Код читают с экрана телевизора и набирают на телефоне, поэтому нормализация подменяет буквы,
 * похожие на цифры. Здесь проверяется главное: подмена не должна калечить сам сгенерированный код —
 * буква из алфавита обязана пережить нормализацию без изменений, иначе верный код будет отвергаться.
 */
class LocalAuthCodeTest {

    @Test
    fun `alphabet survives normalization`() {
        assertEquals(LocalAuthCode.ALPHABET, LocalAuthCode.normalize(LocalAuthCode.ALPHABET))
    }

    @Test
    fun `alphabet has no characters that look like digits`() {
        val confusable = "BGILOQSZ"
        val overlap = LocalAuthCode.ALPHABET.filter { it in confusable }

        assertEquals("", overlap)
    }

    @Test
    fun `lookalikes are mapped to their digits`() {
        assertEquals("86110052", LocalAuthCode.normalize("B6I1OQ5Z"))
        assertEquals("11", LocalAuthCode.normalize("il"))
    }

    @Test
    fun `input is uppercased and cleaned from noise`() {
        assertEquals("AC4D", LocalAuthCode.normalize(" ac-4d! "))
    }

    @Test
    fun `generated code has the expected shape`() {
        repeat(GENERATED_SAMPLES) {
            val code = LocalAuthCrypto.generatePin()

            assertEquals(LocalAuthCode.LENGTH, code.length)
            assertTrue(code, code.all { it in LocalAuthCode.ALPHABET })
            assertEquals(code, LocalAuthCode.normalize(code))
        }
    }

    private companion object {
        const val GENERATED_SAMPLES = 200
    }
}
