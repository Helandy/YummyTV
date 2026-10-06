package su.afk.yummy.tv.feature.account.profileedit

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.EditableProfile
import su.afk.yummy.tv.domain.account.model.LinkedAccountProvider
import su.afk.yummy.tv.domain.account.model.ProfileImageKind
import su.afk.yummy.tv.domain.account.model.ProfileListPrivacy
import su.afk.yummy.tv.domain.account.model.ProfileUpdate
import su.afk.yummy.tv.domain.account.model.UserProfileSex
import su.afk.yummy.tv.feature.account.profileedit.ProfileEditState.Effect
import su.afk.yummy.tv.feature.account.profileedit.ProfileEditState.Event
import su.afk.yummy.tv.feature.account.profileedit.ProfileEditState.MessageType
import su.afk.yummy.tv.feature.account.profileedit.handler.ProfileEditHandler

class ProfileEditViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val handler: ProfileEditHandler = mockk()

    @Before
    fun setUp() {
        coEvery { handler.load() } returns profile()
    }

    private fun createViewModel() = ProfileEditViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        handler = handler,
    )

    private fun profile(
        about: String = "about",
        linked: Set<LinkedAccountProvider> = setOf(LinkedAccountProvider.VK),
    ) = EditableProfile(
        userId = 1,
        nickname = "sandy",
        avatarUrl = "avatar",
        bannerUrl = null,
        about = about,
        birthDateSeconds = 0L,
        sex = UserProfileSex.UNKNOWN,
        listPrivacy = ProfileListPrivacy.PUBLIC,
        showShikimori = true,
        showTelegram = true,
        showVk = true,
        showDiscord = true,
        notifyTelegram = false,
        notifyVk = false,
        linkedAccounts = linked,
    )

    @Test
    fun `loads the profile on start`() {
        val state = createViewModel().currentState

        assertFalse(state.isLoading)
        assertEquals("sandy", state.nickname)
        assertEquals("about", state.about)
        assertEquals(setOf(LinkedAccountProvider.VK), state.linkedAccounts)
    }

    @Test
    fun `failed load shows the load error and retry recovers`() {
        coEvery { handler.load() } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertTrue(vm.currentState.hasLoadError)
        assertFalse(vm.currentState.isLoading)
        coEvery { handler.load() } returns profile()

        vm.setEvent(Event.RetrySelected)

        assertFalse(vm.currentState.hasLoadError)
        assertEquals("sandy", vm.currentState.nickname)
    }

    @Test
    fun `form fields follow the events`() {
        val vm = createViewModel()

        vm.setEvent(Event.AboutChanged("new"))
        vm.setEvent(Event.SexChanged(UserProfileSex.UNKNOWN))
        vm.setEvent(Event.ListPrivacyChanged(ProfileListPrivacy.PRIVATE))
        vm.setEvent(Event.ShowTelegramChanged(false))
        vm.setEvent(Event.NotifyVkChanged(true))

        val state = vm.currentState
        assertEquals("new", state.about)
        assertEquals(ProfileListPrivacy.PRIVATE, state.listPrivacy)
        assertFalse(state.showTelegram)
        assertTrue(state.notifyVk)
    }

    @Test
    fun `save sends the trimmed form and reports success`() = runTest {
        val update = slot<ProfileUpdate>()
        coEvery { handler.update(capture(update)) } returns profile(about = "saved")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.AboutChanged("  saved  "))
        vm.setEvent(Event.ListPrivacyChanged(ProfileListPrivacy.FRIENDS))

        vm.setEvent(Event.SaveSelected)

        assertEquals("saved", update.captured.about)
        assertEquals(ProfileListPrivacy.FRIENDS, update.captured.listPrivacy)
        assertEquals("saved", vm.currentState.about)
        assertFalse(vm.currentState.isSaving)
        assertEquals(listOf<Effect>(Effect.Message(MessageType.PROFILE_SAVED)), effects)
    }

    @Test
    fun `failed save reports the failure and keeps the form`() = runTest {
        coEvery { handler.update(any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.AboutChanged("draft"))

        vm.setEvent(Event.SaveSelected)

        assertEquals("draft", vm.currentState.about)
        assertFalse(vm.currentState.isSaving)
        assertEquals(listOf<Effect>(Effect.Message(MessageType.PROFILE_SAVE_FAILED)), effects)
    }

    @Test
    fun `image upload applies the new profile`() = runTest {
        coEvery { handler.upload(any(), any()) } returns profile()
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.ImageSelected(ProfileImageKind.AVATAR, byteArrayOf(1), "content://preview"))

        coVerify(exactly = 1) { handler.upload(ProfileImageKind.AVATAR, any()) }
        assertNull(vm.currentState.pendingAvatarPreview)
        assertFalse(vm.currentState.isImageLoading)
        assertEquals(listOf<Effect>(Effect.Message(MessageType.IMAGE_SAVED)), effects)
    }

    @Test
    fun `empty image is ignored`() {
        val vm = createViewModel()

        vm.setEvent(Event.ImageSelected(ProfileImageKind.BANNER, byteArrayOf(), "content://preview"))

        coVerify(exactly = 0) { handler.upload(any(), any()) }
    }

    @Test
    fun `failed image upload drops the previews and reports the failure`() = runTest {
        coEvery { handler.upload(any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.ImageSelected(ProfileImageKind.AVATAR, byteArrayOf(1), "content://preview"))

        assertNull(vm.currentState.pendingAvatarPreview)
        assertFalse(vm.currentState.isImageLoading)
        assertEquals(listOf<Effect>(Effect.Message(MessageType.IMAGE_SAVE_FAILED)), effects)
    }

    @Test
    fun `delete image refreshes the profile`() = runTest {
        coEvery { handler.delete(ProfileImageKind.BANNER) } returns profile()
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.DeleteImageSelected(ProfileImageKind.BANNER))

        assertEquals(listOf<Effect>(Effect.Message(MessageType.IMAGE_SAVED)), effects)
    }

    @Test
    fun `mismatching passwords are a validation error`() {
        val vm = createViewModel()
        vm.setEvent(Event.OldPasswordChanged("old"))
        vm.setEvent(Event.NewPasswordChanged("new"))
        vm.setEvent(Event.ConfirmPasswordChanged("other"))

        vm.setEvent(Event.ChangePasswordSelected)

        assertTrue(vm.currentState.passwordValidationError)
        coVerify(exactly = 0) { handler.changePassword(any(), any()) }
    }

    @Test
    fun `password change clears the fields and reports success`() = runTest {
        coEvery { handler.changePassword("old", "new") } returns Unit
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.OldPasswordChanged("old"))
        vm.setEvent(Event.NewPasswordChanged("new"))
        vm.setEvent(Event.ConfirmPasswordChanged("new"))

        vm.setEvent(Event.ChangePasswordSelected)

        assertEquals("", vm.currentState.oldPassword)
        assertEquals("", vm.currentState.newPassword)
        assertFalse(vm.currentState.isPasswordSaving)
        assertEquals(listOf<Effect>(Effect.Message(MessageType.PASSWORD_CHANGED)), effects)
    }

    @Test
    fun `failed password change keeps the fields`() = runTest {
        coEvery { handler.changePassword(any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.OldPasswordChanged("old"))
        vm.setEvent(Event.NewPasswordChanged("new"))
        vm.setEvent(Event.ConfirmPasswordChanged("new"))

        vm.setEvent(Event.ChangePasswordSelected)

        assertEquals("old", vm.currentState.oldPassword)
        assertEquals(listOf<Effect>(Effect.Message(MessageType.PASSWORD_CHANGE_FAILED)), effects)
    }

    @Test
    fun `only a linked account can be marked for unlinking`() {
        val vm = createViewModel()

        vm.setEvent(Event.UnlinkAccountSelected(LinkedAccountProvider.TELEGRAM))
        assertNull(vm.currentState.pendingUnlinkAccount)

        vm.setEvent(Event.UnlinkAccountSelected(LinkedAccountProvider.VK))
        assertEquals(LinkedAccountProvider.VK, vm.currentState.pendingUnlinkAccount)

        vm.setEvent(Event.UnlinkAccountDismissed)
        assertNull(vm.currentState.pendingUnlinkAccount)
    }

    @Test
    fun `confirmed unlink updates the profile and reports success`() = runTest {
        coEvery { handler.unlinkAccount(LinkedAccountProvider.VK) } returns profile(linked = emptySet())
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.UnlinkAccountSelected(LinkedAccountProvider.VK))

        vm.setEvent(Event.UnlinkAccountConfirmed)

        assertTrue(vm.currentState.linkedAccounts.isEmpty())
        assertNull(vm.currentState.pendingUnlinkAccount)
        assertNull(vm.currentState.unlinkingAccount)
        assertEquals(listOf<Effect>(Effect.Message(MessageType.ACCOUNT_UNLINKED)), effects)
    }

    @Test
    fun `failed unlink reports the failure`() = runTest {
        coEvery { handler.unlinkAccount(any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.UnlinkAccountSelected(LinkedAccountProvider.VK))

        vm.setEvent(Event.UnlinkAccountConfirmed)

        assertNull(vm.currentState.unlinkingAccount)
        assertEquals(listOf<Effect>(Effect.Message(MessageType.ACCOUNT_UNLINK_FAILED)), effects)
    }

    @Test
    fun `back leaves the screen`() {
        createViewModel().setEvent(Event.BackSelected)

        verify(exactly = 1) { nav.back() }
    }
}
