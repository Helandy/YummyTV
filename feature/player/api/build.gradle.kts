plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.player.api"
}

dependencies {
    implementation(project(":core:navigation"))
    implementation(project(":core:model"))
    implementation(project(":core:utils"))
    implementation(project(":feature:player:domain"))
    implementation(project(":feature:watching:domain"))
    implementation(libs.javax.inject)

    api(libs.bundles.navigation.serialization)
}
