plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.source.anidub.data"
}

dependencies {
    implementation(project(":source:anidub:domain"))
    implementation(project(":source:availability:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:utils"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:domain"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test.network)
}
