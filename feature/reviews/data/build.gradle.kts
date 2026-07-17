plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.data.reviews" }

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":feature:reviews:domain"))
    implementation(libs.kotlinx.serialization.json)
}
