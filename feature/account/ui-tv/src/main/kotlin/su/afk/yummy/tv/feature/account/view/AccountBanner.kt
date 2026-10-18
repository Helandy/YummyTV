package su.afk.yummy.tv.feature.account.view

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * Баннер профиля фоном под шапкой: картинка (или градиент, если баннера нет)
 * и затемнение слева и снизу, чтобы имя и кнопки читались на любом баннере.
 */
@Composable
internal fun AccountBanner(
    bannerUrl: String?,
    bannerWideUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
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
        val model = if (useWide) bannerWideUrl else bannerUrl?.takeIf { it.isNotBlank() }
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { if (useWide) useWide = false },
                modifier = Modifier.matchParentSize(),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        0f to background.copy(alpha = 0.82f),
                        0.55f to background.copy(alpha = 0.35f),
                        1f to background.copy(alpha = 0.1f),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to background.copy(alpha = 0f),
                        1f to background.copy(alpha = 0.7f),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BannerMinHeight),
            contentAlignment = Alignment.BottomStart,
            content = content,
        )
    }
}

private val BannerMinHeight = 200.dp
