package su.afk.yummy.tv.data.player.extractor.alloha

private const val ACCEPT_LANGUAGE = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7"
private val CHROME_VERSION = Regex("""Chrome/(\d+)""")

/**
 * Headers the CDN wants on every media request. They have to look like one Chrome: a Chrome
 * `User-Agent` with no `Accept-Language` and no client hints earns 403 `client_blocked`, and the
 * player library never adds them itself. The hints are derived from [userAgent] so the two agree.
 */
internal fun allohaCdnHeaders(origin: String, userAgent: String): Map<String, String> = buildMap {
    put("origin", origin)
    put("referer", "$origin/")
    put("user-agent", userAgent)
    put("accept-language", ACCEPT_LANGUAGE)
    put("sec-fetch-dest", "empty")
    put("sec-fetch-mode", "cors")
    put("sec-fetch-site", "cross-site")
    CHROME_VERSION.find(userAgent)?.groupValues?.get(1)?.let { major ->
        val android = userAgent.contains("Android", ignoreCase = true)
        // Chrome on Android lists its brands in this order, the desktop one starts with Chromium.
        put(
            "sec-ch-ua",
            if (android) {
                "\"Google Chrome\";v=\"$major\", \"Chromium\";v=\"$major\", \"Not)A;Brand\";v=\"24\""
            } else {
                "\"Chromium\";v=\"$major\", \"Google Chrome\";v=\"$major\", \"Not_A Brand\";v=\"24\""
            },
        )
        put("sec-ch-ua-mobile", if (userAgent.contains("Mobile")) "?1" else "?0")
        put("sec-ch-ua-platform", platformHintFor(userAgent))
    }
}

private fun platformHintFor(userAgent: String): String = when {
    userAgent.contains("Android", ignoreCase = true) -> "\"Android\""
    userAgent.contains("Windows", ignoreCase = true) -> "\"Windows\""
    userAgent.contains("Macintosh", ignoreCase = true) -> "\"macOS\""
    else -> "\"Linux\""
}
