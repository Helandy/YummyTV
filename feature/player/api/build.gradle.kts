plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.player.api"
}

dependencies {
    implementation(project(":feature:player:domain"))

    api(libs.bundles.navigation.serialization)
}
