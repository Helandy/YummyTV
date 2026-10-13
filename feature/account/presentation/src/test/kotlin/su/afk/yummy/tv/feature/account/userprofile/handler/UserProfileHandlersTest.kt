package su.afk.yummy.tv.feature.account.userprofile.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.model.FriendshipStatus
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.usecase.AddFriendUseCase
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.GetFriendshipUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserFavoriteAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserProfileSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveFriendUseCase
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

class UserProfileHandlersTest : BaseUnitTest() {

    private val getAccountSession: GetAccountSessionUseCase = mockk()
    private val getFriendship: GetFriendshipUseCase = mockk()
    private val addFriend: AddFriendUseCase = mockk(relaxed = true)
    private val removeFriend: RemoveFriendUseCase = mockk(relaxed = true)
    private val getSummary: GetUserProfileSummaryUseCase = mockk()
    private val getStats: GetUserStatsUseCase = mockk()
    private val getFavorites: GetUserFavoriteAnimeListUseCase = mockk()
    private val getAnimeList: GetUserAnimeListUseCase = mockk()

    @Test
    fun `own profile is recognised only for a signed-in viewer`() = runTest {
        val handler = friendshipHandler()

        coEvery { getAccountSession() } returns AccountSession(isAuthorized = true, userId = ME)
        assertEquals(FriendshipOwnership(true, true, ME), handler.resolveOwnership(ME))
        assertEquals(FriendshipOwnership(true, false, ME), handler.resolveOwnership(OTHER))

        coEvery { getAccountSession() } returns AccountSession(isAuthorized = false, userId = ME)
        assertEquals(false, handler.resolveOwnership(ME).isOwnProfile)
    }

    @Test
    fun `friendship button adds or removes depending on the current status`() = runTest {
        coEvery { getFriendship(ME, OTHER) } returns FriendshipStatus.SENT_REQUESTS
        val handler = friendshipHandler()

        assertEquals(FriendshipFetchResult.Success(FriendshipStatus.SENT_REQUESTS), handler.updateFriendship(ME, OTHER, FriendshipStatus.REQUESTS))
        handler.updateFriendship(ME, OTHER, FriendshipStatus.FRIENDS)

        coVerify(exactly = 1) { addFriend(ME, OTHER) }
        coVerify(exactly = 1) { removeFriend(ME, OTHER) }
    }

    @Test
    fun `failed friendship request is reported`() = runTest {
        coEvery { addFriend(ME, OTHER) } throws IllegalStateException("500")

        assertEquals(FriendshipFetchResult.Failure, friendshipHandler().updateFriendship(ME, OTHER, FriendshipStatus.NONE))
    }

    @Test
    fun `favorites tab reads favorites, other tabs read their list`() = runTest {
        coEvery { getFavorites(OTHER, forceRefresh = true) } returns emptyList()
        coEvery { getAnimeList(OTHER, UserAnimeList.DROPPED, forceRefresh = false) } returns emptyList()
        val handler = contentHandler()

        handler.loadLists(OTHER, UserProfileState.ListFilter.FAVORITES, force = true)
        handler.loadLists(OTHER, UserProfileState.ListFilter.DROPPED, force = false)

        coVerify { getFavorites(OTHER, forceRefresh = true) }
        coVerify { getAnimeList(OTHER, UserAnimeList.DROPPED, forceRefresh = false) }
    }

    @Test
    fun `overview fails as a whole when a part fails`() = runTest {
        coEvery { getSummary(OTHER) } returns mockk()
        coEvery { getStats(OTHER) } throws IllegalStateException("stats")

        assertTrue(contentHandler().loadOverview(OTHER).isFailure)
    }

    private fun friendshipHandler() = UserProfileFriendshipHandler(getAccountSession, getFriendship, addFriend, removeFriend)

    private fun contentHandler() = UserProfileContentHandler(getSummary, getStats, getFavorites, getAnimeList)

    private companion object {
        const val ME = 1
        const val OTHER = 2
    }
}
