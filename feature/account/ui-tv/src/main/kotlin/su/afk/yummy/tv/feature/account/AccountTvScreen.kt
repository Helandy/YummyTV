@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.model.UserAnimeTypeStat
import su.afk.yummy.tv.domain.account.model.UserGenreStat
import su.afk.yummy.tv.domain.account.model.UserListWatchStat
import su.afk.yummy.tv.domain.account.model.UserRatingStat
import su.afk.yummy.tv.domain.account.model.UserStats
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountTvScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
) {
    BackHandler { onEvent(AccountState.Event.BackSelected) }
    val horizontalPadding = if (state.isSignedIn) 32.dp else TvScreenPadding.Horizontal

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = horizontalPadding, vertical = TvScreenPadding.Vertical),
    ) {
        if (!state.isSignedIn) {
            LoginContent(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            AccountHubContent(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@Composable
private fun LoginContent(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.74f)
            .widthIn(max = 680.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AccountTitle()
        Text(
            text = stringResource(R.string.account_signed_out),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        OutlinedTextField(
            value = state.login,
            onValueChange = { onEvent(AccountState.Event.LoginChanged(it)) },
            placeholder = { Text(stringResource(R.string.account_login_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = { onEvent(AccountState.Event.PasswordChanged(it)) },
            placeholder = { Text(stringResource(R.string.account_password_placeholder)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        AccountAction(
            label = stringResource(R.string.account_login),
            hint = if (state.isLoading) stringResource(R.string.account_loading) else stringResource(R.string.account_login_hint),
            onClick = { onEvent(AccountState.Event.LoginSelected) },
        )
        if (state.isCaptchaRequired) {
            key(state.captchaChallengeId) {
                CaptchaChallenge(
                    state = state,
                    onSolved = { onEvent(AccountState.Event.CaptchaSolved(it)) },
                    onExpired = { onEvent(AccountState.Event.CaptchaExpired) },
                    onFailed = { onEvent(AccountState.Event.CaptchaFailed(it)) },
                )
            }
        }
        ErrorText(state.error)
    }
}

@Composable
private fun CaptchaChallenge(
    state: AccountState.State,
    onSolved: (String) -> Unit,
    onExpired: () -> Unit,
    onFailed: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.account_captcha_title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = if (state.isLoading) {
                stringResource(R.string.account_captcha_loading)
            } else {
                stringResource(R.string.account_captcha_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HCaptchaWebView(
            siteKey = state.captchaSiteKey,
            onSolved = onSolved,
            onExpired = onExpired,
            onFailed = onFailed,
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(10.dp),
                ),
        )
        ErrorText(state.captchaError)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HCaptchaWebView(
    siteKey: String,
    onSolved: (String) -> Unit,
    onExpired: () -> Unit,
    onFailed: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val html = remember(siteKey) { hCaptchaHtml(siteKey) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isFocusable = true
                isFocusableInTouchMode = true
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                addJavascriptInterface(
                    CaptchaBridge(
                        onSolved = onSolved,
                        onExpired = onExpired,
                        onFailed = onFailed,
                    ),
                    "YummyCaptcha",
                )
                loadDataWithBaseURL(
                    "https://yummyani.me/",
                    html,
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
    )
}

private class CaptchaBridge(
    private val onSolved: (String) -> Unit,
    private val onExpired: () -> Unit,
    private val onFailed: (String?) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onSolved(token: String) {
        handler.post { onSolved(token) }
    }

    @JavascriptInterface
    fun onExpired() {
        handler.post { onExpired() }
    }

    @JavascriptInterface
    fun onError(message: String?) {
        handler.post { onFailed(message) }
    }
}

private fun hCaptchaHtml(siteKey: String): String = """
    <!doctype html>
    <html>
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          html, body {
            margin: 0;
            padding: 0;
            width: 100%;
            min-height: 100%;
            background: transparent;
            color: white;
            overflow: hidden;
          }
          body {
            display: flex;
            align-items: center;
            justify-content: center;
          }
          .h-captcha {
            transform-origin: center;
          }
        </style>
        <script>
          function onCaptchaSolved(token) {
            window.YummyCaptcha.onSolved(token || "");
          }
          function onCaptchaExpired() {
            window.YummyCaptcha.onExpired();
          }
          function onCaptchaError(error) {
            window.YummyCaptcha.onError(error || null);
          }
        </script>
        <script src="https://js.hcaptcha.com/1/api.js" async defer></script>
      </head>
      <body>
        <div
          class="h-captcha"
          data-sitekey="$siteKey"
          data-theme="dark"
          data-callback="onCaptchaSolved"
          data-expired-callback="onCaptchaExpired"
          data-error-callback="onCaptchaError">
        </div>
      </body>
    </html>
""".trimIndent()

@Composable
private fun AccountHubContent(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentModifier = modifier
        .fillMaxHeight()
        .fillMaxWidth(0.92f)
        .widthIn(max = 1440.dp)
    val unreadCount = state.notificationCounts.sumOf { it.count }

    when (state.selectedTab) {
        AccountState.AccountTab.STATS -> StatsTab(
            state = state,
            onEvent = onEvent,
            modifier = contentModifier,
        )
        AccountState.AccountTab.NOTIFICATIONS -> Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            AccountHeader(state = state, onEvent = onEvent)
            AccountTabs(
                selected = state.selectedTab,
                onSelected = { onEvent(AccountState.Event.TabSelected(it)) },
                onMarkAllRead = if (unreadCount > 0) {
                    { onEvent(AccountState.Event.AllNotificationsReadSelected) }
                } else {
                    null
                },
                markAllReadEnabled = !state.isNotificationsLoading,
            )
            ErrorText(state.error ?: state.hubError)
            NotificationsTab(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun AccountHeader(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AccountAvatar(avatarUrl = state.avatarUrl, nickname = state.nickname)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = state.nickname.ifBlank { stringResource(R.string.account_unknown_user) },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val unreadCount = state.notificationCounts.sumOf { it.count }
            if (unreadCount > 0) {
                Text(
                    text = stringResource(R.string.account_unread_count, unreadCount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(modifier = Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountAction(
                label = stringResource(R.string.account_logout),
                hint = stringResource(R.string.account_logout_hint),
                onClick = { onEvent(AccountState.Event.LogoutSelected) },
            )
            AccountAction(
                label = stringResource(R.string.account_refresh),
                hint = if (state.isLoading || state.isStatsLoading || state.isNotificationsLoading) {
                    stringResource(R.string.account_loading)
                } else {
                    stringResource(R.string.account_refresh_hint)
                },
                onClick = { onEvent(AccountState.Event.RefreshProfileSelected) },
            )
        }
    }
}

@Composable
private fun AccountAvatar(avatarUrl: String, nickname: String) {
    Box(
        modifier = Modifier
            .size(92.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun AccountTabs(
    selected: AccountState.AccountTab,
    onSelected: (AccountState.AccountTab) -> Unit,
    onMarkAllRead: (() -> Unit)? = null,
    markAllReadEnabled: Boolean = true,
) {
    val statsFocusRequester = remember { FocusRequester() }
    val notificationsFocusRequester = remember { FocusRequester() }
    val markAllReadFocusRequester = remember { FocusRequester() }

    LaunchedEffect(selected, onMarkAllRead != null) {
        if (selected == AccountState.AccountTab.NOTIFICATIONS && onMarkAllRead != null) {
            runCatching { notificationsFocusRequester.requestFocus() }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AccountTabButton(
                label = stringResource(R.string.account_tab_stats),
                selected = selected == AccountState.AccountTab.STATS,
                onClick = { onSelected(AccountState.AccountTab.STATS) },
                modifier = Modifier
                    .focusRequester(statsFocusRequester)
                    .focusProperties { right = notificationsFocusRequester },
            )
            AccountTabButton(
                label = stringResource(R.string.account_tab_notifications),
                selected = selected == AccountState.AccountTab.NOTIFICATIONS,
                onClick = { onSelected(AccountState.AccountTab.NOTIFICATIONS) },
                modifier = Modifier
                    .focusRequester(notificationsFocusRequester)
                    .focusProperties {
                        left = statsFocusRequester
                        if (onMarkAllRead != null) {
                            right = markAllReadFocusRequester
                        }
                    },
            )
        }
        if (onMarkAllRead != null) {
            AccountAction(
                label = stringResource(R.string.account_mark_all_read),
                onClick = onMarkAllRead,
                modifier = Modifier
                    .width(260.dp)
                    .focusRequester(markAllReadFocusRequester)
                    .focusProperties { left = notificationsFocusRequester },
                enabled = markAllReadEnabled,
            )
        }
    }
}

@Composable
private fun NotificationsPreview(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfacePanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.account_notifications_preview),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            AccountAction(
                label = stringResource(R.string.account_open),
                onClick = { onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.NOTIFICATIONS)) },
                modifier = Modifier.width(140.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (state.isNotificationsLoading && state.notifications.isEmpty()) {
            Text(text = stringResource(R.string.account_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (state.notifications.isEmpty()) {
            EmptyText(stringResource(R.string.account_notifications_empty))
        } else {
            state.notifications.take(3).forEach { notification ->
                NotificationMiniRow(notification)
            }
        }
    }
}

@Composable
private fun StatsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = state.stats
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> {
                        onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.NOTIFICATIONS))
                        true
                    }
                    Key.DirectionDown -> {
                        val last = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                        if (listState.firstVisibleItemIndex >= last) return@onPreviewKeyEvent false
                        val next = (listState.firstVisibleItemIndex + 1).coerceAtMost(last)
                        scope.launch { listState.animateScrollToItem(next) }
                        true
                    }
                    Key.DirectionUp -> {
                        if (listState.firstVisibleItemIndex == 0) return@onPreviewKeyEvent false
                        val previous = (listState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                        scope.launch { listState.animateScrollToItem(previous) }
                        true
                    }
                    else -> false
                }
            },
    ) {
        item {
            AccountHeader(state = state, onEvent = onEvent)
        }
        item {
            AccountTabs(selected = state.selectedTab, onSelected = { onEvent(AccountState.Event.TabSelected(it)) })
        }
        item {
            ErrorText(state.error ?: state.hubError)
        }
        if (state.isStatsLoading && stats == null) {
            item { EmptyText(stringResource(R.string.account_loading)) }
            return@LazyColumn
        }
        if (stats == null || stats.isEmpty()) {
            item { EmptyText(stringResource(R.string.account_stats_empty)) }
            return@LazyColumn
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                stats.lists.forEach { ListStatCard(it) }
            }
        }
        item { GenreStats(stats.genres) }
        item { RatingStats(stats.ratings) }
        item { TypeStats(stats.types) }
    }
}

@Composable
private fun NotificationsTab(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                    onEvent(AccountState.Event.TabSelected(AccountState.AccountTab.STATS))
                    true
                } else {
                    false
                }
        },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.isNotificationsLoading && state.notifications.isEmpty()) {
            EmptyText(stringResource(R.string.account_loading))
        } else if (state.notifications.isEmpty()) {
            EmptyText(stringResource(R.string.account_notifications_empty))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                items(state.notifications, key = { it.id }) { notification ->
                    NotificationRow(
                        notification = notification,
                        onClick = { onEvent(AccountState.Event.NotificationSelected(notification.id)) },
                        onRead = { onEvent(AccountState.Event.NotificationReadSelected(notification.id)) },
                        onDelete = { onEvent(AccountState.Event.NotificationDeleteSelected(notification.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GenreStats(genres: List<UserGenreStat>) {
    StatSection(title = stringResource(R.string.account_stats_genres)) {
        val topGenres = genres.sortedByDescending { it.count }.take(8)
        val max = topGenres.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        topGenres.forEach { genre ->
            StatBar(label = genre.title, valueLabel = genre.count.toString(), fraction = genre.count.toFloat() / max)
        }
    }
}

@Composable
private fun RatingStats(ratings: List<UserRatingStat>) {
    StatSection(title = stringResource(R.string.account_stats_ratings)) {
        val byRating = ratings.associateBy { it.rating }
        val max = ratings.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
        (1..10).forEach { rating ->
            val count = byRating[rating]?.count ?: 0
            StatBar(label = rating.toString(), valueLabel = count.toString(), fraction = count.toFloat() / max)
        }
    }
}

@Composable
private fun TypeStats(types: List<UserAnimeTypeStat>) {
    StatSection(title = stringResource(R.string.account_stats_types)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            types.sortedByDescending { it.count }.forEach { type ->
                StatPill(title = type.title.ifBlank { type.shortName }, value = type.count.toLong().toDurationLabel())
            }
        }
    }
}

@Composable
private fun ListStatCard(stat: UserListWatchStat) {
    SurfacePanel(modifier = Modifier.width(220.dp)) {
        Text(
            text = stat.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stat.seconds.toDurationLabel(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun StatSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    SurfacePanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun StatBar(label: String, valueLabel: String, fraction: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(180.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(5.dp)),
            )
        }
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(52.dp),
        )
    }
}

@Composable
private fun StatPill(title: String, value: String) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(12.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NotificationMiniRow(notification: ProfileNotification) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = notification.title.ifBlank { notification.type },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (notification.viewed) FontWeight.SemiBold else FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = notification.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NotificationRow(
    notification: ProfileNotification,
    onClick: () -> Unit,
    onRead: () -> Unit,
    onDelete: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isOpenable = notification.isNewEpisode && notification.animeSlug != null
    val rowModifier = if (isOpenable) {
        Modifier
            .fillMaxWidth()
            .tvFocusableClick(
                onClick = onClick,
                interactionSource = interactionSource,
                shape = shape,
                focusedScale = 1.01f,
            )
    } else {
        Modifier.fillMaxWidth()
    }

    SurfacePanel(modifier = rowModifier) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp)
                    .size(10.dp)
                    .background(
                        color = if (notification.viewed) Color.Transparent else MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = notification.title.ifBlank { notification.type },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (notification.viewed) FontWeight.SemiBold else FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.dateSeconds.formatDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!notification.viewed) {
                AccountAction(
                    label = stringResource(R.string.account_mark_read),
                    onClick = onRead,
                    modifier = Modifier.width(170.dp),
                )
            }
            AccountAction(
                label = stringResource(R.string.account_delete),
                onClick = onDelete,
                modifier = Modifier.width(140.dp),
            )
        }
    }
}

@Composable
private fun AccountTitle() {
    Text(
        text = stringResource(R.string.account_title),
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun AccountTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccountAction(
        label = label,
        onClick = onClick,
        modifier = modifier.width(210.dp),
        selected = selected,
    )
}

@Composable
private fun AccountAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val active = enabled && (focused || selected)
    val actionModifier = modifier
        .fillMaxWidth()
        .clip(shape)
        .border(
            width = 2.dp,
            color = if (focused && enabled) MaterialTheme.colorScheme.primary else Color.Transparent,
            shape = shape,
        )
        .background(
            color = when {
                focused && enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                selected && enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.025f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
            },
            shape = shape,
        )
        .let {
            if (enabled) {
                it.tvFocusableClick(onClick = onClick, interactionSource = interactionSource, shape = shape)
            } else {
                it
            }
        }
        .padding(horizontal = 16.dp, vertical = 14.dp)
    Row(
        modifier = actionModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    active -> MaterialTheme.colorScheme.primary
                    enabled -> MaterialTheme.colorScheme.onBackground
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SurfacePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f))
            .padding(18.dp),
        content = content,
    )
}

@Composable
private fun ErrorText(error: String?) {
    error?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
    )
}

private fun UserStats.isEmpty(): Boolean =
    genres.isEmpty() && ratings.isEmpty() && lists.isEmpty() && types.isEmpty()

private fun Long.toDurationLabel(): String {
    val hours = this / 3600L
    val minutes = (this % 3600L) / 60L
    val seconds = this % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

private fun Long.formatDate(): String =
    if (this <= 0L) "" else SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(this * 1000L))
