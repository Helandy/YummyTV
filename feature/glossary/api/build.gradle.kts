plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.glossary.api"
}

dependencies {
    implementation(project(":core:navigation"))
    api(libs.bundles.navigation.serialization)
    implementation(libs.javax.inject)
}
