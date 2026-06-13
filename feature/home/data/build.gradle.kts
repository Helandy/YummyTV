plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.data.home"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(project(":core:error"))
    implementation(project(":core:network"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":feature:home:domain"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)

    ksp(libs.hilt.compiler)

    testImplementation(libs.bundles.unit.test.network)
}
