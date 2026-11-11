package su.afk.yummy.tv.core.preferences.settings

import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.model.settings.TorrentSettings

/** Настройки торрент-источника: тумблер, адрес TorrServer и индексатор раздач (JacRed/Jackett). */
interface TorrentSettingsStore {

    val torrentSettings: Flow<TorrentSettings>

    /** Сохраняет все поля одной транзакцией. */
    suspend fun setTorrentSettings(settings: TorrentSettings)
}
