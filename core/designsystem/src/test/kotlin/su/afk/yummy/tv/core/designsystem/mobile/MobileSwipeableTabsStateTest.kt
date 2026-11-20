package su.afk.yummy.tv.core.designsystem.mobile

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileTabTransition

@OptIn(ExperimentalCoroutinesApi::class)
class MobileSwipeableTabsStateTest {

    @Test
    fun `distant jump animates smoothly without flashing intermediate pages and clears on completion`() = runTest {
        val pager = FakeTabPagerAdapter(pageCount = 8, initialPage = 0)
        val state = createState(pager)

        // Прыжок 0 -> 7 (дальше любых внутренних порогов PagerState).
        state.selectPage(7)
        assertEquals(0, state.tabTransition.fromIndex)
        assertEquals(7, state.tabTransition.toIndex)
        assertEquals(0f, state.tabTransition.fraction, DELTA)

        // Имитируем внутренний перескок PagerState на промежуточную страницу 4 во время докрутки.
        pager.currentPage = 4
        pager.currentPageOffsetFraction = 0.3f

        advanceTimeBy(80L)
        runCurrent()

        val mid = state.tabTransition
        assertEquals(0, mid.fromIndex)
        assertEquals(7, mid.toIndex)
        assertTrue("expected mid-flight fraction in (0, 1), was ${mid.fraction}", mid.fraction in 0.05f..0.95f)
        assertEquals(0f, mid.selectionFraction(4), DELTA)

        // Завершаем анимацию индикатора и докрутку pager'а.
        advanceTimeBy(300L)
        runCurrent()
        assertEquals(MobileTabTransition.settled(7), state.tabTransition)

        pager.finishCurrentScroll(settledPage = 7)
        advanceUntilIdle()
        assertEquals(MobileTabTransition.settled(7), state.tabTransition)

        // Проверяем, что programmaticJump сброшен в finally: живой свайп снова читает pager напрямую.
        pager.currentPage = 6
        pager.currentPageOffsetFraction = 0.6f
        val liveSwipe = state.tabTransition
        assertEquals(6, liveSwipe.fromIndex)
        assertEquals(7, liveSwipe.toIndex)
        assertEquals(0.6f, liveSwipe.fraction, DELTA)
    }

    @Test
    fun `mid-jump reversal preserves selection continuity and old finally does not clear new jump`() = runTest {
        val pager = FakeTabPagerAdapter(pageCount = 8, initialPage = 1)
        val state = createState(pager)

        state.selectPage(5)
        advanceTimeBy(64L)
        runCurrent()

        val beforeReversal = state.tabTransition
        assertFalse(beforeReversal.isIdle)
        val tile1Before = beforeReversal.selectionFraction(1)
        val tile5Before = beforeReversal.selectionFraction(5)

        // На полпути 1 -> 5 пользователь передумал и нажал обратно на 1.
        state.selectPage(1)
        val rightAfterReversal = state.tabTransition
        assertEquals(5, rightAfterReversal.fromIndex)
        assertEquals(1, rightAfterReversal.toIndex)
        assertEquals(tile1Before, rightAfterReversal.selectionFraction(1), DELTA)
        assertEquals(tile5Before, rightAfterReversal.selectionFraction(5), DELTA)

        // Даём отменённой первой корутине выполнить свой finally — новый прыжок должен остаться активным.
        runCurrent()
        assertEquals(5, state.tabTransition.fromIndex)
        assertEquals(1, state.tabTransition.toIndex)

        advanceTimeBy(300L)
        pager.finishCurrentScroll(settledPage = 1)
        advanceUntilIdle()

        assertEquals(MobileTabTransition.settled(1), state.tabTransition)
    }

    @Test
    fun `user drag start cancels active programmatic jump and restores live pager tracking`() = runTest {
        val pager = FakeTabPagerAdapter(pageCount = 8, initialPage = 0)
        val state = createState(pager)
        backgroundScope.launch { state.observeUserDragCancellation() }
        runCurrent()

        state.selectPage(6)
        advanceTimeBy(64L)
        runCurrent()
        assertEquals(6, state.tabTransition.toIndex)
        assertFalse(state.tabTransition.isIdle)

        // Пользователь перехватил pager пальцем на странице 3.25.
        pager.currentPage = 3
        pager.currentPageOffsetFraction = 0.25f
        pager.interactions.emit(DragInteraction.Start())
        runCurrent()

        assertTrue(pager.wasLastScrollCancelled)
        val afterDrag = state.tabTransition
        assertEquals(3, afterDrag.fromIndex)
        assertEquals(4, afterDrag.toIndex)
        assertEquals(0.25f, afterDrag.fraction, DELTA)
    }

    @Test
    fun `programmaticJump resets in finally when pager scroll is cancelled externally`() = runTest {
        val pager = FakeTabPagerAdapter(pageCount = 6, initialPage = 0)
        val state = createState(pager)

        state.selectPage(4)
        advanceTimeBy(48L)
        runCurrent()
        assertEquals(4, state.tabTransition.toIndex)

        pager.currentPage = 2
        pager.currentPageOffsetFraction = 0.4f
        pager.cancelCurrentScroll()
        advanceUntilIdle()

        val afterCancel = state.tabTransition
        assertEquals(2, afterCancel.fromIndex)
        assertEquals(3, afterCancel.toIndex)
        assertEquals(0.4f, afterCancel.fraction, DELTA)
    }

    @Test
    fun `adjacent tap follows pager offset directly without creating programmatic jump`() = runTest {
        val pager = FakeTabPagerAdapter(pageCount = 4, initialPage = 1)
        val state = createState(pager)

        state.selectPage(2)
        runCurrent()

        pager.currentPage = 1
        pager.currentPageOffsetFraction = 0.35f
        val transition = state.tabTransition
        assertEquals(1, transition.fromIndex)
        assertEquals(2, transition.toIndex)
        assertEquals(0.35f, transition.fraction, DELTA)

        pager.finishCurrentScroll(settledPage = 2)
        advanceUntilIdle()
        assertEquals(MobileTabTransition.settled(2), state.tabTransition)
    }

    private fun TestScope.createState(
        pager: FakeTabPagerAdapter,
    ): MobileSwipeableTabsState {
        val clock = TestMonotonicFrameClock(testScheduler)
        val stateScope = CoroutineScope(
            SupervisorJob(backgroundScope.coroutineContext.job) +
                StandardTestDispatcher(testScheduler) +
                clock,
        )
        return MobileSwipeableTabsState(pager = pager, scope = stateScope)
    }

    private companion object {
        const val DELTA = 0.001f
    }
}

private class FakeTabPagerAdapter(
    override var pageCount: Int,
    initialPage: Int = 0,
) : TabPagerAdapter {
    override var currentPage: Int = initialPage
    override var currentPageOffsetFraction: Float = 0f
    override var targetPage: Int = initialPage
    override val interactions = MutableSharedFlow<Interaction>(extraBufferCapacity = 4)

    var wasLastScrollCancelled: Boolean = false
        private set

    private var activeGate: CompletableDeferred<Int>? = null

    override suspend fun animateScrollToPage(page: Int) {
        targetPage = page
        val gate = CompletableDeferred<Int>()
        activeGate = gate
        try {
            val settled = gate.await()
            currentPage = settled
            targetPage = settled
            currentPageOffsetFraction = 0f
        } catch (e: CancellationException) {
            wasLastScrollCancelled = true
            throw e
        } finally {
            if (activeGate === gate) {
                activeGate = null
            }
        }
    }

    fun finishCurrentScroll(settledPage: Int) {
        activeGate?.complete(settledPage)
    }

    fun cancelCurrentScroll() {
        activeGate?.cancel(CancellationException("interrupted"))
    }
}

private class TestMonotonicFrameClock(
    private val scheduler: TestCoroutineScheduler,
    private val frameDelayMs: Long = 16L,
) : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        delay(frameDelayMs)
        return onFrame(scheduler.currentTime * 1_000_000L)
    }
}
