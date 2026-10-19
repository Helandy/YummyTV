package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseBottomSheet
import su.afk.yummy.tv.feature.details.presentation.R

/** Шторка (i) у названия: другие названия тайтла, по одному на строку. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsInfoSheet(
    titles: List<String>,
    onDismiss: () -> Unit,
) {
    BaseBottomSheet(
        onDismissRequest = onDismiss,
        titleContent = {
            Text(
                text = stringResource(R.string.details_mobile_other_titles),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
            )
        },
        verticalArrangement = Arrangement.spacedBy(10.dp),
        scrollableContent = true,
    ) {
        titles.forEach { Text(text = it, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
    }
}
