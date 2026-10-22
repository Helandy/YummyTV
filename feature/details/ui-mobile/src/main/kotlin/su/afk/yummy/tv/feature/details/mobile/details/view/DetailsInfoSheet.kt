package su.afk.yummy.tv.feature.details.mobile.details.view

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.core.utils.system.copyToClipboard
import su.afk.yummy.tv.feature.details.mobile.details.utils.groupTitlesByScript
import su.afk.yummy.tv.feature.details.presentation.R

/** Шторка (i) у названия: другие названия тайтла, сгруппированные по письменности. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsInfoSheet(
    titles: List<String>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val copyLabel = stringResource(R.string.details_mobile_other_titles)
    val groups = remember(titles) { titles.groupTitlesByScript() }
    BaseBottomSheet(
        onDismissRequest = onDismiss,
        titleContent = {
            Text(
                text = stringResource(R.string.details_mobile_other_titles),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
            )
        },
        verticalArrangement = Arrangement.spacedBy(16.dp),
        scrollableContent = true,
    ) {
        groups.forEach { (script, items) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(script.labelRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp),
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        items.forEachIndexed { index, title ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (context.copyToClipboard(copyLabel, title)) {
                                            Toast.makeText(context, R.string.details_mobile_title_copied, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
