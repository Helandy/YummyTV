package su.afk.yummy.tv.domain.catalog.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.catalog.repository.SearchHistoryRepository

class AddSearchHistoryQueryUseCaseTest : BaseUnitTest() {

    private val repository: SearchHistoryRepository = mockk()
    private var history: List<String> = emptyList()

    @Before
    fun setUp() {
        coEvery { repository.update(any()) } answers {
            history = firstArg<(List<String>) -> List<String>>().invoke(history)
        }
    }

    @Test
    fun `new query goes first trimmed and replaces the same query in another case`() = runTest {
        history = listOf("Naruto", "Bleach")

        AddSearchHistoryQueryUseCase(repository)("  bleach ")

        assertEquals(listOf("bleach", "Naruto"), history)
    }

    @Test
    fun `history keeps only the last ten queries`() = runTest {
        history = (1..10).map { "q$it" }

        AddSearchHistoryQueryUseCase(repository)("new")

        assertEquals(listOf("new") + (1..9).map { "q$it" }, history)
    }

    @Test
    fun `blank query is not stored`() = runTest {
        AddSearchHistoryQueryUseCase(repository)("   ")

        coVerify(exactly = 0) { repository.update(any()) }
    }
}
