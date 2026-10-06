package su.afk.yummy.tv.core.preferences.settings.datastore

import su.afk.yummy.tv.core.model.settings.NewEpisodesSource

/**
 * Маркер «пользователь снял все списки»: пустое множество в DataStore неотличимо от отсутствия
 * записи, поэтому осознанный пустой выбор пишем этим значением.
 */
internal const val NO_NEW_EPISODES_SOURCES = "NONE"

/**
 * Хранимые имена списков в [NewEpisodesSource]. Пустое значение означает, что настройку ещё не
 * трогали — тогда действует набор по умолчанию.
 */
internal fun Set<String>?.toNewEpisodesSources(): Set<NewEpisodesSource> =
    if (isNullOrEmpty()) {
        NewEpisodesSource.DEFAULT
    } else {
        mapNotNullTo(mutableSetOf()) { name ->
            NewEpisodesSource.entries.firstOrNull { it.name == name }
        }
    }

/** Обратное преобразование для записи; пустой выбор сохраняем маркером. */
internal fun Set<NewEpisodesSource>.toStoredNames(): Set<String> =
    mapTo(mutableSetOf()) { it.name }.ifEmpty { setOf(NO_NEW_EPISODES_SOURCES) }
