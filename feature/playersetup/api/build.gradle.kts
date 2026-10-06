plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.playersetup.api"
}

dependencies {
    implementation(project(":core:navigation"))
    api(libs.bundles.navigation.serialization)
}
