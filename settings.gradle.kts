rootProject.name = "YummyTv"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":app")
include(":baselineprofile")

include(":core:navigation")
include(":core:analytics")
include(":core:utils")
include(":core:designsystem")
include(":core:network")
include(":core:preferences")
include(":core:preferences:api")
include(":core:storage")
include(":core:featuretoggle")
include(":core:tv")
include(":core:tv:api")
include(":core:deeplink")
include(":core:deeplink:api")
include(":core:model")
include(":core:common")
include(":core:testing")
include(":core:error")
include(":core:error:api")
include(":core:mvi")

include(":feature:main:api")
include(":feature:main:presentation")
include(":feature:main:ui-tv")
include(":feature:main:ui-mobile")

include(":feature:commonScreen:api")
include(":feature:commonScreen:presenter")

include(":feature:home:api")
include(":feature:catalog:api")
include(":feature:library:api")
include(":feature:details:api")
include(":feature:player:api")
include(":feature:settings:api")
include(":feature:playersetup:api")

include(":feature:home:domain")
include(":feature:home:data")
include(":feature:home:presentation")
include(":feature:home:ui-tv")
include(":feature:home:ui-mobile")

include(":feature:details:domain")
include(":feature:details:data")
include(":feature:details:presentation")
include(":feature:details:ui-tv")
include(":feature:details:ui-mobile")

include(":feature:comments:api")
include(":feature:comments:domain")
include(":feature:comments:data")
include(":feature:comments:presentation")
include(":feature:comments:ui-mobile")
include(":feature:comments:ui-tv")

include(":feature:reviews:api")
include(":feature:reviews:domain")
include(":feature:reviews:data")
include(":feature:reviews:presentation")
include(":feature:reviews:ui-mobile")
include(":feature:reviews:ui-tv")

include(":feature:posts:api")
include(":feature:posts:domain")
include(":feature:posts:data")
include(":feature:posts:presentation")
include(":feature:posts:ui-mobile")
include(":feature:posts:ui-tv")

include(":feature:messages:api")
include(":feature:messages:domain")
include(":feature:messages:data")
include(":feature:messages:presentation")
include(":feature:messages:ui-mobile")

include(":feature:bloggers:api")
include(":feature:bloggers:domain")
include(":feature:bloggers:data")
include(":feature:bloggers:presentation")
include(":feature:bloggers:ui-mobile")
include(":feature:bloggers:ui-tv")

include(":feature:catalog:domain")
include(":feature:catalog:data")
include(":feature:catalog:presentation")
include(":feature:catalog:ui-tv")
include(":feature:catalog:ui-mobile")
include(":feature:catalog:android")

include(":feature:player:domain")
include(":feature:player:data")
include(":feature:player:presentation")
include(":feature:player:ui-common")
include(":feature:player:ui-tv")
include(":feature:player:ui-mobile")

include(":feature:settings:presentation")
include(":feature:settings:ui-common")
include(":feature:settings:ui-tv")
include(":feature:settings:ui-mobile")

include(":feature:playersetup:presentation")
include(":feature:playersetup:ui-tv")
include(":feature:playersetup:ui-mobile")

include(":feature:faq:api")
include(":feature:faq:presentation")
include(":feature:faq:ui-mobile")
include(":feature:glossary:api")
include(":feature:glossary:presentation")
include(":feature:glossary:ui-mobile")

include(":feature:pages:api")
include(":feature:pages:domain")
include(":feature:pages:data")
include(":feature:pages:presentation")
include(":feature:pages:ui-mobile")

include(":feature:video-download:api")
include(":feature:video-download:domain")
include(":feature:video-download:data")
include(":feature:video-download:presentation")
include(":feature:video-download:ui-mobile")

include(":feature:watch-later:api")
include(":feature:watch-later:domain")
include(":feature:watch-later:data")
include(":feature:watch-later:presentation")
include(":feature:watch-later:ui-mobile")

include(":feature:library:presentation")
include(":feature:library:domain")
include(":feature:library:data")
include(":feature:library:ui-tv")
include(":feature:library:ui-mobile")

include(":feature:watching:domain")

include(":feature:account:domain")
include(":feature:account:data")
include(":feature:account:api")
include(":feature:account:presentation")
include(":feature:account:ui-common")
include(":feature:account:ui-tv")
include(":feature:account:ui-mobile")

include(":feature:collection:api")
include(":feature:collection:domain")
include(":feature:collection:data")
include(":feature:collection:presentation")
include(":feature:collection:ui-tv")
include(":feature:collection:ui-mobile")

include(":feature:schedule:api")
include(":feature:schedule:domain")
include(":feature:schedule:data")
include(":feature:schedule:presentation")
include(":feature:schedule:ui-tv")
include(":feature:schedule:ui-mobile")

include(":feature:update:api")
include(":feature:update:domain")
include(":feature:update:data")
include(":feature:update:presentation")
include(":feature:update:ui")

// Переключатели в gradle.properties: yummytv.source.enabled (все source/*) и yummytv.source.<имя> (один модуль).
// Выключенный модуль не подключается к сборке; зависящие от него модули выключаются следом.
fun sourceEnabled(name: String): Boolean =
    providers.gradleProperty("yummytv.source.enabled").orNull != "false" &&
        providers.gradleProperty("yummytv.source.$name").orNull != "false" &&
        file("source/$name").isDirectory

// Необязательные модули-источники. Каталог удаляется целиком, остальное приложение собирается как раньше.
if (sourceEnabled("anime4k")) {
    include(":source:anime4k:domain")
    include(":source:anime4k:data")
}

if (sourceEnabled("availability")) {
    include(":source:availability:domain")
    include(":source:availability:data")
}

if (sourceEnabled("ailiberty") && sourceEnabled("availability")) {
    include(":source:ailiberty:domain")
    include(":source:ailiberty:data")
}

if (sourceEnabled("anidub") && sourceEnabled("availability")) {
    include(":source:anidub:domain")
    include(":source:anidub:data")
}

if (sourceEnabled("sameband") && sourceEnabled("availability")) {
    include(":source:sameband:domain")
    include(":source:sameband:data")
}

if (sourceEnabled("studiomir") && sourceEnabled("availability")) {
    include(":source:studiomir:domain")
    include(":source:studiomir:data")
}

if (sourceEnabled("ashdi")) {
    include(":source:ashdi:domain")
    include(":source:ashdi:data")
}

if (sourceEnabled("fenix")) {
    include(":source:fenix:domain")
    include(":source:fenix:data")
}

if (sourceEnabled("moon")) {
    include(":source:moon:domain")
    include(":source:moon:data")
}

if (sourceEnabled("animeon") && sourceEnabled("availability") && sourceEnabled("moon")) {
    include(":source:animeon:domain")
    include(":source:animeon:data")
}

if (sourceEnabled("animevost") && sourceEnabled("availability")) {
    include(":source:animevost:domain")
    include(":source:animevost:data")
}

if (sourceEnabled("mikai") && sourceEnabled("availability") && sourceEnabled("ashdi") &&
    sourceEnabled("fenix") && sourceEnabled("moon")
) {
    include(":source:mikai:data")
}

if (sourceEnabled("animebesst") && sourceEnabled("availability")) {
    include(":source:animebesst:domain")
    include(":source:animebesst:data")
}

if (sourceEnabled("dreamerscast") && sourceEnabled("availability")) {
    include(":source:dreamerscast:domain")
    include(":source:dreamerscast:data")
}

if (sourceEnabled("aniboom")) {
    include(":source:aniboom:domain")
    include(":source:aniboom:data")
}

if (sourceEnabled("anistar")) {
    include(":source:anistar:domain")
    include(":source:anistar:data")
}

if (sourceEnabled("amedia") && sourceEnabled("availability")) {
    include(":source:amedia:domain")
    include(":source:amedia:data")
}

if (sourceEnabled("anilibria") && sourceEnabled("availability")) {
    include(":source:anilibria:domain")
    include(":source:anilibria:data")
}

// Внешние сервисы не-видео (external/*): yummytv.external.<имя> в gradle.properties. Нет каталога или false — модуль не подключается.
fun externalEnabled(name: String): Boolean =
    providers.gradleProperty("yummytv.external.$name").orNull != "false" &&
        file("external/$name").isDirectory

if (externalEnabled("tenrai")) {
    include(":external:tenrai")
}
