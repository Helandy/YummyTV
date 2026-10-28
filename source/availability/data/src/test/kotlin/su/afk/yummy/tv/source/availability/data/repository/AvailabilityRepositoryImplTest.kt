package su.afk.yummy.tv.source.availability.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.availability.data.dto.AvailabilityResponseDto
import su.afk.yummy.tv.source.availability.data.dto.SourceAvailabilityDto
import su.afk.yummy.tv.source.availability.data.network.AvailabilityApi
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import java.io.IOException

class AvailabilityRepositoryImplTest : BaseUnitTest() {

    private val api: AvailabilityApi = mockk()
    private val lookup = AvailabilityLookup(1, "A", emptyList(), null, null, null)
    private val response = AvailabilityResponseDto(listOf(SourceAvailabilityDto("ailiberty", "k", "https://x/k", 3)))

    private fun createRepository() = AvailabilityRepositoryImpl(api)

    @Test
    fun `parallel requests for one title make a single call`() = runTest {
        coEvery { api.find(any()) } returns response
        val repository = createRepository()

        val answers = (1..5).map { async { repository.find(lookup) } }.awaitAll()

        assertEquals(5, answers.size)
        coVerify(exactly = 1) { api.find(any()) }
    }

    @Test
    fun `answer is cached per title`() = runTest {
        coEvery { api.find(any()) } returns response
        val repository = createRepository()

        repository.find(lookup)
        repository.find(lookup)
        repository.find(lookup.copy(malId = 2))

        coVerify(exactly = 2) { api.find(any()) }
    }

    @Test
    fun `answer with failed sources and network errors are not cached`() = runTest {
        coEvery { api.find(any()) } returns response.copy(failed = listOf("slow"))
        val repository = createRepository()
        repository.find(lookup)
        repository.find(lookup)
        coVerify(exactly = 2) { api.find(any()) }

        coEvery { api.find(any()) } throws IOException("down")
        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { repository.find(lookup.copy(malId = 9)) } }
        coEvery { api.find(any()) } returns response
        repository.find(lookup.copy(malId = 9))
        coVerify(exactly = 4) { api.find(any()) }
    }
}
