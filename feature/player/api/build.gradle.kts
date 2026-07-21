plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.player.api"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":feature:player:domain"))
    implementation(project(":feature:watching:domain"))

    api(libs.bundles.navigation.serialization)
}
