package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage

suspend fun YaniAccountSettingsStore.currentLanguageCode(): String =
    yaniContentLanguage.first().apiCode

/**
 * Реальные смены языка контента: стартовое значение не приходит, поэтому подписчик перезагружает
 * данные только когда язык действительно изменился.
 */
fun YaniAccountSettingsStore.contentLanguageChanges(): Flow<YaniContentLanguage> =
    yaniContentLanguage.distinctUntilChanged().drop(1)

suspend fun YaniAccountSettingsStore.currentUserId(): Int =
    yaniUserId.first()
