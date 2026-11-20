package su.afk.yummy.tv.feature.account.mobile.account

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.components.OfflineBanner
import su.afk.yummy.tv.core.designsystem.locals.LocalIsOffline
import su.afk.yummy.tv.core.designsystem.mobile.MobileSectionHeader
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileBottomBarUpFocusRequester
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.layout.mobileContentMaxWidth
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.mvi.collectAsEffect
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileQuickAction
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileHeader
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoginCard
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLogoutConfirmDialog
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLogoutProgressDialog
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileQuickActionsRow
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileStatsTab
import su.afk.yummy.tv.feature.account.presentation.R

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AccountMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        AccountMobileScreen(AccountState.State(isSessionResolved = true), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun AccountMobileScreenLoadingPreview() = ScreenPreviewTheme {
    AccountMobileScreen(AccountState.State(isSessionResolved = true, isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun AccountMobileScreenErrorPreview() = ScreenPreviewTheme {
    AccountMobileScreen(
        AccountState.State(isSessionResolved = true, error = AccountUiError.SIGN_IN_FAILED),
        emptyFlow(),
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountMobileScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val autofillManager = LocalAutofillManager.current
    val context = LocalContext.current
    val captchaHint = stringResource(R.string.account_captcha_required_toast)
    LaunchedEffect(Unit) { onEvent(AccountState.Event.ScreenShown) }

    effect.collectAsEffect { effectItem ->
        when (effectItem) {
            AccountState.Effect.HideKeyboard -> {
                keyboardController?.hide()
                focusManager.clearFocus()
            }

            AccountState.Effect.ShowCaptchaHint -> Toast.makeText(context, captchaHint, Toast.LENGTH_SHORT).show()
            AccountState.Effect.DiscardAutofill -> autofillManager?.cancel()
        }
    }

    val isOffline = LocalIsOffline.current
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val bottomBarUpFocusRequester = LocalMobileBottomBarUpFocusRequester.current

    BaseScreen(
        isScroll = false,
        // Верхний инсет не берём: шапка профиля заходит под статус-бар, остальные состояния отступают сами.
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
    ) {
        if (!state.isSessionResolved) {
            // Пока не пришёл первый снапшот сессии, не мигаем формой входа авторизованному пользователю.
            AccountMobileLoadingIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            )
        } else if (!state.isSignedIn) {
            val quickActions = listOf(
                faqAction(onEvent),
                glossaryAction(onEvent),
                downloadedAction(onEvent),
                AccountMobileQuickAction(
                    key = "settings",
                    title = stringResource(R.string.account_settings),
                    icon = Icons.Outlined.Settings,
                    onClick = { onEvent(AccountState.Event.SettingsSelected) },
                    focusRequester = bottomBarUpFocusRequester,
                ),
            )
            LazyColumn(
                modifier = Modifier
                    .mobileContentMaxWidth()
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = MobileBottomBarDefaults.contentBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "quick_actions") {
                    AccountMobileQuickActionsRow(actions = quickActions)
                }
                item {
                    AccountMobileLoginCard(state = state, onEvent = onEvent)
                }
            }
        } else {
            val quickActions = listOf(
                faqAction(onEvent),
                glossaryAction(onEvent),
                downloadedAction(onEvent),
                AccountMobileQuickAction(
                    key = "notifications",
                    title = stringResource(R.string.account_tab_notifications),
                    icon = Icons.Outlined.Notifications,
                    onClick = { onEvent(AccountState.Event.NotificationsSelected) },
                    focusRequester = bottomBarUpFocusRequester,
                    badgeCount = state.unreadNotificationCount,
                ),
            )
            // Шапка на всю ширину и под статус-баром; остальным блокам отступ по бокам задаём сами.
            LazyColumn(
                modifier = Modifier
                    .mobileContentMaxWidth()
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = MobileBottomBarDefaults.contentBottomPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "profile") {
                    // Баннер остаётся под статус-баром, плашка лежит поверх него, а кнопки и аватар сдвинуты вниз.
                    var bannerHeightPx by remember { mutableIntStateOf(0) }
                    Box {
                        AccountMobileHeader(
                            state = state,
                            profileSummary = state.profileSummary,
                            onEditClick = { onEvent(AccountState.Event.ProfileEditSelected) },
                            onSettingsClick = { onEvent(AccountState.Event.SettingsSelected) },
                            onLogoutClick = { showLogoutConfirm = true },
                            topExtra = with(LocalDensity.current) { bannerHeightPx.toDp() },
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .onSizeChanged { bannerHeightPx = it.height },
                        ) {
                            OfflineBanner(isOffline = isOffline)
                        }
                    }
                }
                item(key = "quick_actions") {
                    AccountMobileQuickActionsRow(
                        actions = quickActions,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                if (state.hubError != null) {
                    item(key = "hub_error") {
                        state.hubError.accountErrorMessage()?.let { error ->
                            MobileMessage(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                title = error,
                                icon = Icons.Filled.Warning,
                                actionLabel = stringResource(CoreR.string.common_retry),
                                onAction = { onEvent(AccountState.Event.RefreshHubSelected) },
                                fillMaxSize = false,
                            )
                        }
                    }
                }
                item(key = "stats") {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MobileSectionHeader(title = stringResource(R.string.account_tab_stats))
                        AccountMobileStatsTab(
                            profileSummary = state.profileSummary,
                            stats = state.stats,
                            isLoading = state.isStatsLoading,
                        )
                    }
                }
            }
        }
    }

    if (state.isLogoutInProgress) {
        AccountMobileLogoutProgressDialog()
    }

    if (showLogoutConfirm) {
        AccountMobileLogoutConfirmDialog(
            onConfirm = {
                showLogoutConfirm = false
                onEvent(AccountState.Event.LogoutSelected)
            },
            onDismiss = { showLogoutConfirm = false },
        )
    }
}

@Composable
private fun faqAction(onEvent: (AccountState.Event) -> Unit) = AccountMobileQuickAction(
    key = "faq",
    title = stringResource(R.string.account_faq_group),
    icon = Icons.AutoMirrored.Outlined.HelpOutline,
    onClick = { onEvent(AccountState.Event.FaqSelected) },
)

@Composable
private fun glossaryAction(onEvent: (AccountState.Event) -> Unit) = AccountMobileQuickAction(
    key = "glossary",
    title = stringResource(R.string.account_glossary),
    icon = Icons.Outlined.MenuBook,
    onClick = { onEvent(AccountState.Event.GlossarySelected) },
)

@Composable
private fun downloadedAction(onEvent: (AccountState.Event) -> Unit) = AccountMobileQuickAction(
    key = "downloaded_episodes",
    title = stringResource(R.string.account_downloaded_episodes),
    icon = Icons.Outlined.VideoLibrary,
    onClick = { onEvent(AccountState.Event.DownloadedEpisodesSelected) },
)
