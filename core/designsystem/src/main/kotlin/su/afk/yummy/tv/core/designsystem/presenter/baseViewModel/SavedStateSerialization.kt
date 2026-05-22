package su.afk.yummy.tv.core.designsystem.presenter.baseViewModel

import androidx.lifecycle.SavedStateHandle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@PublishedApi
internal val stateJson = Json { ignoreUnknownKeys = true }

inline fun <reified T : Any> SavedStateHandle.getSerializableState(key: String): T? {
    val json = get<String>(key) ?: return null
    return runCatching { stateJson.decodeFromString<T>(json) }.getOrNull()
}

inline fun <reified T : Any> SavedStateHandle.setSerializableState(key: String, value: T?) {
    if (value == null) {
        remove<String>(key)
    } else {
        set(key, stateJson.encodeToString(value))
    }
}
