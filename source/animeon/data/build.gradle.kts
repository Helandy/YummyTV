plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.source.animeon.data"
}

dependencies {
    implementation(project(":source:animeon:domain"))
    implementation(project(":source:ashdi:domain"))
    implementation(project(":source:fenix:domain"))
    implementation(project(":source:availability:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:domain"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test.network)
}
