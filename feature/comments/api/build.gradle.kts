plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "su.afk.yummy.tv.feature.comments.api"
}

dependencies {
    api(project(":feature:comments:domain"))
    api(libs.bundles.navigation.serialization)
}
