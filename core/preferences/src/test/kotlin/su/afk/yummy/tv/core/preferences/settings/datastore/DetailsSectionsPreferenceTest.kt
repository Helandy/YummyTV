package su.afk.yummy.tv.core.preferences.settings.datastore

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.DetailsSectionConfig
import su.afk.yummy.tv.core.model.settings.DetailsSectionKind

class DetailsSectionsPreferenceTest {

    @Test
    fun `missing value gives defaults`() {
        assertEquals(DetailsSectionConfig.default, null.toDetailsSections())
    }

    @Test
    fun `round trip keeps order and visibility`() {
        val source = DetailsSectionConfig.default.reversed().map {
            if (it.kind == DetailsSectionKind.SCREENSHOTS) it.copy(enabled = false) else it
        }

        assertEquals(source, source.toDetailsSectionsPreference().toDetailsSections())
    }

    @Test
    fun `unknown and duplicate entries are dropped and missing ones appended`() {
        val restored = "COMMENTS|GONE|COMMENTS:0|VIDEO".toDetailsSections()

        assertEquals(DetailsSectionKind.entries.size, restored.size)
        assertEquals(listOf(DetailsSectionKind.COMMENTS, DetailsSectionKind.VIDEO), restored.map { it.kind }.filter {
            it == DetailsSectionKind.COMMENTS || it == DetailsSectionKind.VIDEO
        })
        assertEquals(true, restored.first { it.kind == DetailsSectionKind.COMMENTS }.enabled)
    }

    @Test
    fun `missing sections are inserted at their default place`() {
        val saved = DetailsSectionConfig.default
            .filterNot { it.kind == DetailsSectionKind.INFO || it.kind == DetailsSectionKind.DESCRIPTION }
            .reversed()

        val restored = saved.toDetailsSectionsPreference().toDetailsSections()

        assertEquals(
            listOf(DetailsSectionKind.INFO, DetailsSectionKind.DESCRIPTION) + saved.map { it.kind },
            restored.map { it.kind },
        )
    }
}
