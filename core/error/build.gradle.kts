plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "su.afk.yummy.tv.core.error"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.android)

    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)

    debugImplementation(libs.compose.uiTooling)

    implementation(project(":core:navigation"))
    api(project(":core:model"))
    implementation(project(":feature:commonScreen:api"))
}
