plugins {
    id("yummytv.android.library.compose")
    alias(libs.plugins.kotlinSerialization)
    id("yummytv.android.hilt")
}

val appVersionName = providers.gradleProperty("yummytv.versionName").get()

android {
    namespace = "su.afk.yummy.tv.feature.settings"
    defaultConfig {
        buildConfigField("String", "VERSION_NAME", "\"$appVersionName\"")
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:utils"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:settings:presentation"))

    implementation(libs.bundles.compose.core)
    implementation(libs.bundles.navigation.serialization)

    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
}
