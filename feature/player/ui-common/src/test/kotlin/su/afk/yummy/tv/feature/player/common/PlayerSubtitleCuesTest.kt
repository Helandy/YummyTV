package su.afk.yummy.tv.feature.player.common

import androidx.media3.common.text.Cue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Одновременные реплики должны стоять друг над другом, а не поверх друг друга (issue #23). */
class PlayerSubtitleCuesTest {

    @Test
    fun `две одновременные реплики склеиваются в один cue`() {
        val result = listOf(textCue("— Привет!"), textCue("— Как дела?")).mergeSimultaneousCues()

        assertEquals(1, result.size)
        assertEquals("— Привет!\n— Как дела?", result.single().text.toString())
    }

    @Test
    fun `дубликаты слоёв ASS схлопываются`() {
        val cues = listOf(textCue("реплика"), textCue(" реплика "), textCue("другая"))

        assertEquals(listOf("реплика", "другая"), cues.subtitleLines())
    }

    @Test
    fun `пустые реплики отбрасываются`() {
        val cues = listOf(textCue("реплика"), textCue("   "))

        assertEquals(listOf("реплика"), cues.subtitleLines())
    }

    @Test
    fun `одиночная реплика остаётся собой`() {
        val result = listOf(textCue("реплика")).mergeSimultaneousCues()

        assertEquals(1, result.size)
        assertEquals("реплика", result.single().text.toString())
    }

    @Test
    fun `склеенный cue позиционируется отступом плеера, а не координатами дорожки`() {
        val positioned = Cue.Builder()
            .setText("реплика")
            .setLine(0.1f, Cue.LINE_TYPE_FRACTION)
            .setPosition(0.8f)
            .build()

        val merged = listOf(positioned, textCue("вторая")).mergeSimultaneousCues().single()

        assertEquals(Cue.DIMEN_UNSET, merged.line)
        assertEquals(Cue.TYPE_UNSET, merged.lineType)
        assertEquals(Cue.DIMEN_UNSET, merged.position)
    }

    @Test
    fun `пустой список остаётся пустым`() {
        assertEquals(emptyList<Cue>(), emptyList<Cue>().mergeSimultaneousCues())
    }

    private fun textCue(text: String): Cue = Cue.Builder().setText(text).build()
}
