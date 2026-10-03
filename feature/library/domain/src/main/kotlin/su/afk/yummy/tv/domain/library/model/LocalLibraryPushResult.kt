package su.afk.yummy.tv.domain.library.model

internal data class LocalLibraryPushResult(
    val changedRemote: Boolean,
    val error: Throwable?,
)
