import su.afk.yummy.tv.buildlogic.buildConfigObfuscatedSecret
import su.afk.yummy.tv.buildlogic.buildConfigSecret

plugins {
    id("yummytv.android.library")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.source.anime4k.data"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField(
            "String",
            "YANDEX_REWARDED_AD_UNIT_ID",
            buildConfigSecret("yummytv.yandexRewardedAdUnitId"),
        )
        buildConfigField(
            "String",
            "API_SIGNING_SECRET",
            buildConfigObfuscatedSecret(
                "yummytv.apiSigningSecret",
                "su.afk.yummy.tv.source.anime4k.data.signing.SecretDecoder",
            ),
        )
    }
}

dependencies {
    implementation(project(":source:anime4k:domain"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:featuretoggle"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:utils"))
    implementation(project(":feature:details:domain"))
    implementation(project(":feature:player:domain"))

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    // Версия намеренно не в общем каталоге: удаление каталога source/anime4k не оставляет следов.
    implementation("com.yandex.android:mobileads:8.6.0")

    testImplementation(project(":core:testing"))
    testImplementation(libs.bundles.unit.test.network)
    testImplementation(libs.ktor.client.contentNegotiation)
    testImplementation(libs.ktor.serialization.kotlinxJson)
}
