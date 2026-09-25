plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.update.api"
}

dependencies {
    implementation(project(":core:navigation"))
    api(libs.bundles.navigation.serialization)
}
