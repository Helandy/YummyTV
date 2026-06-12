package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.domain.player.isAksorPlayerUrl as domainIsAksorPlayerUrl
import su.afk.yummy.tv.domain.player.isAllohaPlayerUrl as domainIsAllohaPlayerUrl
import su.afk.yummy.tv.domain.player.isCvhPlayerUrl as domainIsCvhPlayerUrl
import su.afk.yummy.tv.domain.player.isKodikPlayerUrl as domainIsKodikPlayerUrl
import su.afk.yummy.tv.domain.player.isRutubePlayerUrl as domainIsRutubePlayerUrl
import su.afk.yummy.tv.domain.player.isSupportedPlayerUrl as domainIsSupportedPlayerUrl
import su.afk.yummy.tv.domain.player.isVkPlayerUrl as domainIsVkPlayerUrl

fun String.isKodikPlayerUrl(): Boolean =
    domainIsKodikPlayerUrl()

fun String.isAksorPlayerUrl(): Boolean =
    domainIsAksorPlayerUrl()

fun String.isCvhPlayerUrl(): Boolean =
    domainIsCvhPlayerUrl()

fun String.isAllohaPlayerUrl(): Boolean =
    domainIsAllohaPlayerUrl()

fun String.isVkPlayerUrl(): Boolean =
    domainIsVkPlayerUrl()

fun String.isRutubePlayerUrl(): Boolean =
    domainIsRutubePlayerUrl()

fun String.isSupportedPlayerUrl(): Boolean =
    domainIsSupportedPlayerUrl()
