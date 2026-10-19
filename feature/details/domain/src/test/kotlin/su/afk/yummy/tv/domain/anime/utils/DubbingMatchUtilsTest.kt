package su.afk.yummy.tv.domain.anime.utils

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkipSegment
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DubbingMatchUtilsTest {

    private val yummy = listOf(
        "Озвучка AniLibria",
        "Озвучка AniDUB Online",
        "Озвучка SHIZA Project",
        "Озвучка Dream Cast",
        "Озвучка Дубляж ОРТ",
        "Субтитры Wakanim",
    )

    @Test
    fun `domain suffix and case are ignored`() {
        assertEquals("Озвучка AniLibria", matchDubbingName("anilibria.tv", yummy))
        assertEquals("Озвучка AniLibria", matchDubbingName("AniLibria", yummy))
    }

    @Test
    fun `noise words and spacing are ignored`() {
        assertEquals("Озвучка AniDUB Online", matchDubbingName("Anidub", yummy))
        assertEquals("Озвучка SHIZA Project", matchDubbingName("Shiza", yummy))
        assertEquals("Озвучка Dream Cast", matchDubbingName("DreamCast", yummy))
    }

    @Test
    fun `ambiguous loose match is not merged`() {
        val pool = listOf("Озвучка AniDUB", "Озвучка AniDUB Online")
        assertEquals("Озвучка AniDUB", matchDubbingName("AniDUB", pool))
        assertNull(matchDubbingName("Anidub Studio", pool))
    }

    @Test
    fun `subtitles and voice are not mixed`() {
        assertEquals("Субтитры Wakanim", matchDubbingName("Субтитры Wakanim", yummy))
    }

    @Test
    fun `composite names match as sets`() {
        val pool = listOf("Озвучка AniLibria & Shiza")
        assertEquals("Озвучка AniLibria & Shiza", matchDubbingName("Shiza / AniLibria", pool))
    }

    @Test
    fun `merge keeps source url and renames matched dubbing`() {
        val base = listOf(video(1, "Озвучка AniLibria", "AniLibria", "https://kodik"))
        val extras = listOf(
            video(-1, "anilibria.tv", "4K", "source://anime4k/1/1/1/anilibria.tv"),
            video(-2, "Unknown Team", "4K", "source://anime4k/1/1/1/Unknown"),
        )
        val merged = mergeExtraDubbings(base, extras)
        assertEquals("Озвучка AniLibria", merged[0].dubbing)
        assertEquals("source://anime4k/1/1/1/anilibria.tv", merged[0].iframeUrl)
        assertEquals("Unknown Team", merged[1].dubbing)
    }

    @Test
    fun `real 4k names from the live api`() {
        val pool = listOf(
            "Озвучка Animedia", "Озвучка AniMedia", "Озвучка StudioBand", "Озвучка Дубляж StudioBand",
            "Озвучка КОМНАТА ДИДИ (Альтернативная)", "Озвучка Комната Диди", "Озвучка Дублированная",
            "Озвучка ТО Дубляжная", "Озвучка Дубляж ТО Дубляжная", "Озвучка Дубляж Мосфильм-Мастер",
            "Озвучка Мосфильм", "Субтитры Crunchyroll", "Озвучка Crunchyroll", "Субтитры Netflix",
        )
        assertEquals("Озвучка Animedia", matchDubbingName("Animedia", pool))
        assertEquals("Озвучка StudioBand", matchDubbingName("Studio Band", pool))
        assertEquals("Озвучка Комната Диди", matchDubbingName("КОМНАТА ДИДИ", pool))
        assertEquals("Озвучка КОМНАТА ДИДИ (Альтернативная)", matchDubbingName("КОМНАТА ДИДИ альтернативная", pool))
        assertEquals("Озвучка Дублированная", matchDubbingName("Дублированный", pool))
        assertEquals("Озвучка ТО Дубляжная", matchDubbingName("Дубляжная", pool))
        assertEquals("Озвучка Дубляж Мосфильм-Мастер", matchDubbingName("Мосфильм-Мастер", pool))
        assertEquals("Субтитры Crunchyroll", matchDubbingName("Crunchyroll.Subtitles", pool))
        assertEquals("Субтитры Netflix", matchDubbingName("Netflix.Subtitles", pool))
        assertEquals("Озвучка Freedub", matchDubbingName("FreedubStudio", pool + "Озвучка Freedub"))
        assertEquals("Субтитры Kazoku Project", matchDubbingName("Kazoku Project|Субтитры", pool + "Субтитры Kazoku Project" + "Озвучка Kazoku Project"))
        assertEquals("Субтитры", matchDubbingName("Субтитры в 4К", pool + "Субтитры"))
        assertEquals("Субтитры Wakanim", matchDubbingName("Wakanim", pool + "Субтитры Wakanim"))
        assertNull(matchDubbingName("AniLibria.Subtitles", pool + "Озвучка AniLibria"))
        assertNull(matchDubbingName("Дубляж ORD Studio [4K]", pool))
        assertNull(matchDubbingName("USB Voice", pool))
    }

    @Test
    fun `extra video takes skips of the same dubbing and episode`() {
        val opening = AnimeVideoSkipSegment(startMs = 60_000L, endMs = 150_000L)
        val skips = AnimeVideoSkips(opening = opening)
        val base = listOf(
            video(1, "Озвучка AniLibria", "Kodik", "k1").copy(episode = "01"),
            video(2, "Озвучка AniLibria", "CVH", "c1").copy(episode = "01", skips = skips),
            video(3, "Озвучка AniLibria", "CVH", "c2").copy(episode = "02"),
        )
        val extras = listOf(
            video(-1, "AniLibria", "4K", "e1").copy(episode = "1"),
            video(-2, "AniLibria", "4K", "e2").copy(episode = "2"),
            video(-3, "Unknown Team", "4K", "e3").copy(episode = "1"),
        )
        val merged = mergeExtraDubbings(base, extras)
        assertEquals(skips, merged[0].skips)
        assertEquals(AnimeVideoSkips(), merged[1].skips)
        assertEquals(AnimeVideoSkips(), merged[2].skips)
    }

    @Test
    fun `extra video keeps its own skips`() {
        val own = AnimeVideoSkips(opening = AnimeVideoSkipSegment(startMs = 1_000L, endMs = 2_000L))
        val other = AnimeVideoSkips(opening = AnimeVideoSkipSegment(startMs = 60_000L, endMs = 150_000L))
        val base = listOf(video(1, "Озвучка AniLibria", "Kodik", "k1").copy(skips = other))
        val extras = listOf(video(-1, "AniLibria", "4K", "e1").copy(skips = own))
        assertEquals(own, mergeExtraDubbings(base, extras)[0].skips)
    }

    @Test
    fun `retain base dubbings drops unmatched extras`() {
        val base = listOf(video(1, "Озвучка AniLibria", "Kodik", "https://kodik"))
        val extras = listOf(
            video(-1, "AniLibria", "AniLibria", "source://anilibria/1/1"),
            video(-2, "Unknown Team", "AniLibria", "source://anilibria/1/2"),
        )
        val kept = retainBaseDubbings(base, mergeExtraDubbings(base, extras))
        assertEquals(listOf("source://anilibria/1/1"), kept.map { it.iframeUrl })
        assertEquals("Озвучка AniLibria", kept[0].dubbing)
    }

    @Test
    fun `retain base dubbings drops everything when base is empty`() {
        val extras = listOf(video(-1, "AniLibria", "AniLibria", "source://anilibria/1/1"))
        assertEquals(emptyList<AnimeVideo>(), retainBaseDubbings(emptyList(), mergeExtraDubbings(emptyList(), extras)))
    }

    private fun video(id: Int, dubbing: String, player: String, url: String) = AnimeVideo(
        id = id,
        episode = "1",
        dubbing = dubbing,
        player = player,
        playerId = null,
        iframeUrl = url,
        durationSeconds = null,
    )
}
