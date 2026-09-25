plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.comments.api"
}

dependencies {
    implementation(project(":core:navigation"))
    api(project(":feature:comments:domain"))
    api(libs.bundles.navigation.serialization)
    implementation(libs.javax.inject)
}
