plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android { namespace = "su.afk.yummy.tv.feature.messages.api" }

dependencies {
    implementation(project(":core:navigation"))
    implementation(libs.bundles.navigation.serialization)
    implementation(libs.javax.inject)
}
