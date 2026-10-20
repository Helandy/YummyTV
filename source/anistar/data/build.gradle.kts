plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.source.anistar.data"
}

dependencies {
    implementation(project(":source:anistar:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:domain"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test.network)
    testImplementation(libs.ktor.client.contentNegotiation)
    testImplementation(libs.ktor.serialization.kotlinxJson)
}
