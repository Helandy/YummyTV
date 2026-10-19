package su.afk.yummy.tv.core.network.yani

import io.ktor.http.Url

const val YANI_BASE_URL = "https://api.yani.tv"

/** Хост встроенного [YANI_BASE_URL]: выводится из него, отдельно не задаётся. */
internal val YANI_API_HOST: String = Url(YANI_BASE_URL).host
