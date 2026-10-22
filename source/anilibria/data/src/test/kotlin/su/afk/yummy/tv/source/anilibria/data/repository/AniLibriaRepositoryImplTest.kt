package su.afk.yummy.tv.source.anilibria.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaExternalIdDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaNameDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaReleaseDto
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaSearchItemDto
import su.afk.yummy.tv.source.anilibria.data.network.AniLibriaApi

class AniLibriaRepositoryImplTest : BaseUnitTest() {

    private val api: AniLibriaApi = mockk()

    private fun createRepository() = AniLibriaRepositoryImpl(api)

    private fun item(id: Int, mal: Int?, english: String, year: Int) = AniLibriaSearchItemDto(
        id = id,
        mal = mal?.let(::AniLibriaExternalIdDto),
        name = AniLibriaNameDto(main = "ru $english", english = english),
        year = year,
    )

    private fun stubRelease(id: Int, mal: Int?) {
        coEvery { api.getRelease(id) } returns AniLibriaReleaseDto(id = id, mal = mal?.let(::AniLibriaExternalIdDto))
    }

    @Test
    fun `release is matched by mal id and latin titles are searched first`() = runTest {
        coEvery { api.search(any()) } returns emptyList()
        coEvery { api.search("Sousou no Frieren") } returns listOf(item(9542, 52991, "Sousou no Frieren", 2023))
        stubRelease(9542, 52991)

        val release = createRepository().findRelease(52991, listOf("Фрирен", "Sousou no Frieren"), 2023)

        assertEquals(9542, release?.id)
    }

    @Test
    fun `season suffix is stripped for a later query`() = runTest {
        coEvery { api.search(any()) } returns emptyList()
        coEvery { api.search("Sousou no Frieren") } returns listOf(item(10085, 59978, "Sousou no Frieren 2nd Season", 2026))
        stubRelease(10085, 59978)

        val release = createRepository().findRelease(59978, listOf("Sousou no Frieren 2nd Season"), 2026)

        assertEquals(10085, release?.id)
    }

    @Test
    fun `release without mal id is taken by exact title and year`() = runTest {
        coEvery { api.search(any()) } returns listOf(item(1, null, "Some Show", 2020))
        stubRelease(1, null)

        val release = createRepository().findRelease(5, listOf("Some Show"), 2021)

        assertEquals(1, release?.id)
    }

    @Test
    fun `release without mal id is rejected for another year or when ambiguous`() = runTest {
        coEvery { api.search(any()) } returns listOf(item(1, null, "Some Show", 2010))
        assertNull(createRepository().findRelease(5, listOf("Some Show"), 2021))

        coEvery { api.search(any()) } returns listOf(item(1, null, "Some Show", 2020), item(2, null, "Some Show", 2021))
        assertNull(createRepository().findRelease(6, listOf("Some Show"), 2021))
    }
}
