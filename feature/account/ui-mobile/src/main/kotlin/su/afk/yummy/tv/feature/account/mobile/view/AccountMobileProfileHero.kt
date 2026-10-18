package su.afk.yummy.tv.feature.account.mobile.view

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Шапка профиля: баннер (или градиент, если баннера нет), аватар с обводкой,
 * наполовину заходящий на баннер, и произвольное содержимое под ним.
 */
@Composable
internal fun AccountMobileProfileHero(
    bannerUrl: String,
    bannerWideUrl: String?,
    avatarUrl: String,
    nickname: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    /** Скругление низа карточки; 0 — шапка продолжается следующим блоком (вкладки чужого профиля). */
    bottomCorner: Dp = HeroCorner,
    /** Слот слева сверху — например кнопка «назад» на чужом профиле. */
    navigationAction: @Composable BoxScope.() -> Unit = {},
    topActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surfaceContainer
    // Баннер на всю ширину и под статус-баром, как фон деталки.
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bannerHeight = statusBarTop + BannerHeight
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = bottomCorner, bottomEnd = bottomCorner))
            .background(surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bannerHeight)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                        ),
                    ),
                ),
        ) {
            // Широкую панораму (AVIF) платформа декодирует с Android 12; иначе или при ошибке — cropped.
            var useWide by remember(bannerWideUrl) {
                mutableStateOf(!bannerWideUrl.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            }
            val model = if (useWide) bannerWideUrl else bannerUrl.takeIf { it.isNotBlank() }
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onError = { if (useWide) useWide = false },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // Затемнение под статус-баром — чтобы его иконки читались на светлом баннере.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarTop + 32.dp)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent))),
            )
            // Лёгкое затемнение только у нижней кромки: картинку не съедаем, переход в карточку мягкий.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.8f to Color.Transparent,
                            1f to surface.copy(alpha = 0.85f),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp),
                content = navigationAction,
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                content = topActions,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = bannerHeight - (AvatarSize + AvatarRing * 2) / 3, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(AvatarSize + AvatarRing * 2)
                    .clip(CircleShape)
                    .background(surface)
                    .padding(AvatarRing),
            ) {
                AccountMobileAvatar(
                    avatarUrl = avatarUrl,
                    nickname = nickname,
                    isLoading = isLoading,
                    modifier = Modifier.size(AvatarSize),
                )
            }
            content()
        }
    }
}

private val BannerHeight = 140.dp
private val HeroCorner = 28.dp
private val AvatarSize = 80.dp
private val AvatarRing = 4.dp
