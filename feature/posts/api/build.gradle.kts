plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
}

android { namespace = "su.afk.yummy.tv.feature.posts.api" }

dependencies { implementation(libs.bundles.navigation.serialization) }
