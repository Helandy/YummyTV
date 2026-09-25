plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.data.account"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:network"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:utils"))
    implementation(project(":feature:account:domain"))

    implementation(libs.kotlinx.coroutines.playServices)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.contentNegotiation)
    implementation(libs.ktor.serialization.kotlinxJson)
    implementation(libs.play.services.auth.blockstore)

    testImplementation(libs.junit)
    testImplementation(libs.bundles.unit.test)
}
