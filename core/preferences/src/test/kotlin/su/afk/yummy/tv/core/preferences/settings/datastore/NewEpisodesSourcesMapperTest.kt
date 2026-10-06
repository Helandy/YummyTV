package su.afk.yummy.tv.core.preferences.settings.datastore

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.core.testing.BaseUnitTest

class NewEpisodesSourcesMapperTest : BaseUnitTest() {

    @Test
    fun `a missing value means the setting was never touched`() {
        assertEquals(NewEpisodesSource.DEFAULT, null.toNewEpisodesSources())
    }

    @Test
    fun `an empty stored set also falls back to the default`() {
        assertEquals(NewEpisodesSource.DEFAULT, emptySet<String>().toNewEpisodesSources())
    }

    @Test
    fun `the marker reads back as a deliberately empty choice`() {
        // Иначе снятые пользователем галочки выглядели бы как «настройку не открывали».
        assertEquals(emptySet<NewEpisodesSource>(), setOf(NO_NEW_EPISODES_SOURCES).toNewEpisodesSources())
    }

    @Test
    fun `an empty choice is stored as the marker`() {
        assertEquals(setOf(NO_NEW_EPISODES_SOURCES), emptySet<NewEpisodesSource>().toStoredNames())
    }

    @Test
    fun `a selection survives the round trip`() {
        val sources = setOf(NewEpisodesSource.PLANNED, NewEpisodesSource.FAVORITES)

        assertEquals(sources, sources.toStoredNames().toNewEpisodesSources())
    }

    @Test
    fun `an empty choice survives the round trip`() {
        val sources = emptySet<NewEpisodesSource>()

        assertEquals(sources, sources.toStoredNames().toNewEpisodesSources())
    }

    @Test
    fun `unknown names are ignored`() {
        val stored = setOf(NewEpisodesSource.WATCHING.name, "RENAMED_IN_A_FUTURE_VERSION")

        assertEquals(setOf(NewEpisodesSource.WATCHING), stored.toNewEpisodesSources())
    }
}
