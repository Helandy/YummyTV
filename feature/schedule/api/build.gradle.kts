plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.schedule.api"
}

dependencies {
    api(project(":core:analytics"))
    api(libs.bundles.navigation.serialization)
}
