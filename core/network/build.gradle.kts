plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.network"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:preferences:api"))
    implementation(project(":core:utils"))

    api(libs.ktor.client.core)
    api(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)

    implementation(libs.bundles.ktor.client.json)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test.network)
}
