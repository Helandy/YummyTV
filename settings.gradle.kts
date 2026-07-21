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
        maven { url = uri("https://jitpack.io") }
    }
}

include(":app")

include(":core:navigation")
include(":core:logger")
include(":core:analytics")
include(":core:utils")
include(":core:designsystem")
include(":core:network")
include(":core:preferences")
include(":core:storage")
include(":core:update")
include(":core:featuretoggle")
include(":core:tv:tv-api")
include(":core:deeplink")
include(":core:tv:tv-impl")
include(":core:model")
include(":core:error")

include(":feature:main:api")
include(":feature:main:presentation")
include(":feature:main:ui-tv")
include(":feature:main:ui-mobile")

include(":feature:commonScreen:api")
include(":feature:commonScreen:presenter")


include(":feature:home:api")
include(":feature:search:api")
include(":feature:top:api")
include(":feature:library:api")
include(":feature:details:api")
include(":feature:player:api")
include(":feature:settings:api")

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

include(":feature:search:domain")
include(":feature:search:data")
include(":feature:search:presentation")
include(":feature:search:ui-tv")
include(":feature:search:ui-mobile")

include(":feature:top:domain")
include(":feature:top:data")
include(":feature:top:presentation")
include(":feature:top:ui-tv")
include(":feature:top:ui-mobile")

include(":feature:player:domain")
include(":feature:player:data")
include(":feature:player:presentation")
include(":feature:player:ui-common")
include(":feature:player:ui-tv")
include(":feature:player:ui-mobile")

include(":feature:settings:presentation")
include(":feature:settings:ui-tv")
include(":feature:settings:ui-mobile")

include(":feature:faq:api")
include(":feature:faq:ui-mobile")

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
