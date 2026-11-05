import su.afk.yummy.tv.buildlogic.buildConfigProperty

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
    defaultConfig {
        buildConfigField("String", "STATIC_HOST", buildConfigProperty("yummytv.staticHost", "static.self.made"))
        buildConfigField("String", "YUMMY_TV_API_BASE_URL", buildConfigProperty("yummytv.apiBaseUrl", "https://api.self.made/api"))
    }
}

dependencies {
    implementation(project(":core:preferences:api"))
    api(project(":core:common"))
    implementation(project(":core:utils"))

    api(libs.ktor.client.core)
    api(libs.okhttp)
    implementation(libs.okhttp.dnsoverhttps)

    implementation(libs.bundles.ktor.client.json)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test.network)
}
