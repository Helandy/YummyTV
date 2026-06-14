plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.library.api"
}

dependencies {
    api(project(":core:analytics"))
}

dependencies {
    api(libs.bundles.navigation.serialization)
}
