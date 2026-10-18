@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.domain.account.model.UserProfileSex
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.feature.account.common.utils.formatProfileDate
import su.afk.yummy.tv.feature.account.common.utils.label

/** Имя, мета-чипы (регистрация, день рождения, пол) и «О себе» под аватаром шапки. */
@Composable
internal fun AccountMobileProfileIdentity(
    nickname: String?,
    summary: UserProfileSummary?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (nickname != null) {
            Text(
                text = nickname,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (summary != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AccountMobileProfileMetaChip(
                    icon = Icons.Filled.CalendarMonth,
                    value = summary.registerDateSeconds.formatProfileDate(),
                )
                AccountMobileProfileMetaChip(
                    icon = Icons.Filled.Cake,
                    value = summary.birthDateSeconds.formatProfileDate(),
                )
                if (summary.sex != UserProfileSex.UNKNOWN) {
                    AccountMobileProfileMetaChip(icon = Icons.Filled.Person, value = summary.sex.label())
                }
            }
            if (summary.about.isNotBlank()) {
                AccountMobileProfileAbout(text = summary.about, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }
}
