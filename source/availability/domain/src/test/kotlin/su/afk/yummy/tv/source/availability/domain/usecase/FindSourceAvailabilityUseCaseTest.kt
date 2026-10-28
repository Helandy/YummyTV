package su.afk.yummy.tv.source.availability.domain.usecase

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityAnswer
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.model.SourceAvailability
import su.afk.yummy.tv.source.availability.domain.repository.AvailabilityRepository
import java.io.IOException

class FindSourceAvailabilityUseCaseTest : BaseUnitTest() {

    private val repository: AvailabilityRepository = mockk()
    private val lookup = AvailabilityLookup(1, "A", emptyList(), null, null, null)
    private val found = SourceAvailability("ailiberty", "k", "https://x/k", 3, 12, true, listOf("720p"))

    private fun createUseCase() = FindSourceAvailabilityUseCase(repository)

    @Test
    fun `returns the answer of the requested source only`() = runTest {
        coEvery { repository.find(lookup) } returns AvailabilityAnswer(listOf(found, found.copy(sourceId = "other")), emptySet())

        assertEquals(found, createUseCase()("ailiberty", lookup))
    }

    @Test
    fun `source without the title gives null`() = runTest {
        coEvery { repository.find(lookup) } returns AvailabilityAnswer(emptyList(), emptySet())

        assertNull(createUseCase()("ailiberty", lookup))
    }

    @Test
    fun `source that did not answer is a failure and not an absence`() = runTest {
        coEvery { repository.find(lookup) } returns AvailabilityAnswer(emptyList(), setOf("ailiberty"))

        assertThrows(IOException::class.java) { kotlinx.coroutines.runBlocking { createUseCase()("ailiberty", lookup) } }
    }
}
