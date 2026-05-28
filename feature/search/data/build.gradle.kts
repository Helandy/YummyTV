plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.data.search"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":feature:search:domain"))
    implementation(project(":core:network"))
    implementation(project(":core:storage"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
