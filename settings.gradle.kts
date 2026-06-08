rootProject.name = "YummyTv"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
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
include(":core:utils")
include(":core:designsystem")
include(":core:network")
include(":core:preferences")
include(":core:storage")
include(":core:update")
include(":core:tv-api")
include(":core:deeplink")
include(":core:tv")
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

include(":feature:player:presentation")
include(":feature:player:ui-tv")
include(":feature:player:ui-mobile")

include(":feature:settings:presentation")
include(":feature:settings:ui-tv")
include(":feature:settings:ui-mobile")

include(":feature:library:presentation")
include(":feature:library:ui-tv")
include(":feature:library:ui-mobile")

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
