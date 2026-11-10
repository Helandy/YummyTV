package su.afk.yummy.tv.core.designsystem.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.R

private class WelcomeSlide(
    val icon: ImageVector,
    @param:StringRes val titleRes: Int,
    @param:StringRes val textRes: Int,
)

private fun welcomeSlides() = listOf(
    WelcomeSlide(Icons.Rounded.Movie, R.string.onboarding_welcome_catalog_title, R.string.onboarding_welcome_catalog_text),
    WelcomeSlide(Icons.Rounded.Bookmarks, R.string.onboarding_welcome_lists_title, R.string.onboarding_welcome_lists_text),
    WelcomeSlide(
        Icons.Rounded.RecordVoiceOver,
        R.string.onboarding_welcome_dubbing_title,
        R.string.onboarding_welcome_dubbing_text,
    ),
    WelcomeSlide(
        Icons.Rounded.PlayCircle,
        R.string.onboarding_welcome_player_title,
        R.string.onboarding_welcome_player_text,
    ),
)

/**
 * Приветственные слайды при первом запуске (только мобильный интерфейс). Закрытие любым способом (кнопка, «Пропустить», Back)
 * вызывает [onFinished]: слайды больше не показываются, после них стартует спотлайт-тур.
 */
@Composable
fun OnboardingWelcome(onFinished: () -> Unit) {
    val slides = remember { welcomeSlides() }
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == slides.lastIndex

    Dialog(
        onDismissRequest = onFinished,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { page ->
                    WelcomeSlidePage(slides[page])
                }
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(slides.size) { index ->
                        val color by animateColorAsState(
                            if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                            label = "welcomeDot",
                        )
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    }
                }
                Row(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OnboardingButton(
                        text = stringResource(R.string.onboarding_skip),
                        accent = false,
                        onClick = onFinished,
                    )
                    OnboardingButton(
                        text = stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next),
                        accent = true,
                        onClick = {
                            if (isLast) {
                                onFinished()
                            } else {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeSlidePage(slide: WelcomeSlide) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = slide.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(88.dp),
        )
        Text(
            text = stringResource(slide.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = stringResource(slide.textRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 12.dp)
                .widthIn(max = 520.dp),
        )
    }
}
