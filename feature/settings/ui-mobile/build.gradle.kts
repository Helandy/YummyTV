plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

val appVersionName = providers.gradleProperty("yummytv.versionName").get()

android {
    namespace = "su.afk.yummy.tv.feature.settings.mobile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        buildConfigField("String", "VERSION_NAME", "\"$appVersionName\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:preferences"))
    implementation(project(":feature:settings:api"))
    implementation(project(":feature:settings:presentation"))

    implementation(libs.bundles.compose.screen)
    implementation(libs.bundles.navigation.serialization)

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.hilt.android)

    ksp(libs.hilt.compiler)
}
