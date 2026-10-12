package su.afk.yummy.tv.core.preferences.settings.datastore

import android.content.Context
import android.os.Build
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Битый файл заменяется пустыми настройками: иначе каждая запись (`edit`) падает с
// CorruptionException на каждом запуске, а чтение в `data` лишь маскирует проблему.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Единственная точка доступа к DataStore настроек: делегат `preferencesDataStore` допускает ровно
 * одно создание хранилища на процесс, поэтому доменные хранилища
 * ([DataStorePlayerSettingsStore] и остальные) работают не с `Context`, а с этим объектом.
 */
@Singleton
internal class SettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /** Подменяет повреждённый/нечитаемый DataStore пустыми настройками вместо падения потока. */
    val data: Flow<Preferences> = context.dataStore.data.catch { cause ->
        if (cause is IOException || cause is CorruptionException) {
            emit(emptyPreferences())
        } else {
            throw cause
        }
    }

    suspend fun edit(transform: suspend (MutablePreferences) -> Unit) {
        context.dataStore.edit(transform)
    }

    fun boolean(key: Preferences.Key<Boolean>, default: Boolean): Flow<Boolean> =
        data.map { prefs -> prefs[key] ?: default }

    fun string(key: Preferences.Key<String>): Flow<String> =
        data.map { prefs -> prefs[key].orEmpty() }

    fun int(key: Preferences.Key<Int>, default: Int = 0): Flow<Int> =
        data.map { prefs -> prefs[key] ?: default }

    fun stringSet(key: Preferences.Key<Set<String>>): Flow<Set<String>> =
        data.map { prefs -> prefs[key].orEmpty() }

    /**
     * Flow-обёртка над [enum]. `inline`/`reified`, т.к. внутри вызывает reified-[enum]
     * (reified-тип «прокидывается» только через inline-функции).
     */
    inline fun <reified T : Enum<T>> enumFlow(
        key: Preferences.Key<String>,
        default: T,
    ): Flow<T> = data.map { prefs -> prefs.enum(key, default) }

    /**
     * Пишет enum по имени константы. Здесь `reified` не нужен — для записи хватает `value.name`,
     * рантайм-класс enum не требуется, поэтому обычный дженерик с границей `T : Enum<T>`.
     */
    suspend fun <T : Enum<T>> setEnum(key: Preferences.Key<String>, value: T) {
        edit { prefs -> prefs[key] = value.name }
    }

    suspend fun setBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
        edit { prefs -> prefs[key] = value }
    }

    /**
     * Системный язык как [YaniContentLanguage] по умолчанию, когда язык контента ещё не выбран
     * пользователем. Живёт здесь (а не в [YaniContentLanguage]), чтобы enum оставался чистым
     * Kotlin-типом без зависимости на [Context].
     */
    fun resolveSystemContentLanguage(): YaniContentLanguage {
        val languageCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)?.language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale?.language
        }
        return YaniContentLanguage.entries.firstOrNull {
            it.apiCode.equals(languageCode, ignoreCase = true)
        } ?: YaniContentLanguage.DEFAULT
    }
}
