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

include(":tvApp")

include(":core:navigation")
include(":core:utils")
include(":core:designsystem")
include(":core:network")
include(":core:storage")
include(":core:update")
include(":core:deeplink")
include(":core:tv")
include(":core:model")
include(":core:error")

include(":feature:main:api")
include(":feature:main:presentation")
include(":feature:main:ui-tv")

include(":feature:commonScreen:api")
include(":feature:commonScreen:presenter")

include(":feature:home:api")
include(":feature:search:api")
include(":feature:top100:api")
include(":feature:library:api")
include(":feature:details:api")
include(":feature:player:api")
include(":feature:settings:api")

include(":feature:home:domain")
include(":feature:home:data")
include(":feature:home:presentation")
include(":feature:home:ui-tv")

include(":feature:details:domain")
include(":feature:details:data")
include(":feature:details:presentation")
include(":feature:details:ui-tv")

include(":feature:search:domain")
include(":feature:search:data")
include(":feature:search:presentation")
include(":feature:search:ui-tv")

include(":feature:top100:domain")
include(":feature:top100:data")
include(":feature:top100:presentation")
include(":feature:top100:ui-tv")

include(":feature:player:presentation")
include(":feature:player:ui-tv")

include(":feature:settings:presentation")
include(":feature:settings:ui-tv")

include(":feature:library:presentation")
include(":feature:library:ui-tv")

include(":feature:account:domain")
include(":feature:account:data")
include(":feature:account:api")
include(":feature:account:presentation")
include(":feature:account:ui-tv")

include(":feature:collection:api")
include(":feature:collection:domain")
include(":feature:collection:data")
include(":feature:collection:presentation")
include(":feature:collection:ui-tv")

include(":feature:schedule:api")
include(":feature:schedule:domain")
include(":feature:schedule:data")
include(":feature:schedule:presentation")
include(":feature:schedule:ui-tv")
