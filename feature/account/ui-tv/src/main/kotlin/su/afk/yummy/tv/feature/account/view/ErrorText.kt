@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun ErrorText(error: String?) {
    error?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}
