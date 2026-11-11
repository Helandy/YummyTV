package su.afk.yummy.tv.source.torrent.domain.model

private val VIDEO_EXTENSIONS = setOf("mkv", "mp4", "avi", "m4v", "webm", "ts", "m2ts", "mov", "wmv", "flv")

/** Имя файла без каталогов раздачи. */
val TorrentFile.fileName: String
    get() = path.substringAfterLast('/')

/** Видеофайл по расширению: субтитры, картинки и прочее в выбор серии не попадают. */
val TorrentFile.isVideo: Boolean
    get() = fileName.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

/**
 * Насколько файл похож на серию [episode]: `5` ищется как отдельное число в имени (`[05]`, ` - 5 `, `E05`),
 * а не как часть другого (`1080`, `2024`). Больше — лучше; 0 — не нашли.
 */
fun TorrentFile.episodeMatchScore(episode: String): Int {
    val number = episode.trim().toIntOrNull() ?: return 0
    val numbers = Regex("""(?<!\d)(\d{1,4})(?!\d)""").findAll(fileName).mapNotNull { it.value.toIntOrNull() }
    // Годы и разрешение (720, 1080, 2160, 19xx/20xx) не считаем номером серии.
    return if (numbers.any { it == number && it !in NON_EPISODE_NUMBERS && it !in 1900..2100 }) 1 else 0
}

private val NON_EPISODE_NUMBERS = setOf(240, 360, 480, 576, 720, 1080, 1440, 2160, 4320)

/**
 * Файл, который можно играть без вопроса пользователю: единственный видеофайл раздачи или единственный из
 * нескольких, чьё имя содержит номер серии [episode]. Если подходящих несколько или ни одного — `null`
 * (пользователь выберет сам): ошибиться в серии хуже, чем спросить.
 */
fun List<TorrentFile>.autoSelectFor(episode: String): TorrentFile? {
    val videos = filter { it.isVideo }
    if (videos.size == 1) return videos.single()
    return videos.filter { it.episodeMatchScore(episode) > 0 }.singleOrNull()
}
