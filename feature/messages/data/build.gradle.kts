plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android { namespace = "su.afk.yummy.tv.data.messages" }

dependencies {
    implementation(project(":core:network"))
    implementation(project(":core:utils"))
    implementation(project(":feature:messages:domain"))
    implementation(libs.kotlinx.serialization.json)
}
