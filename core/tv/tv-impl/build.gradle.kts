plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.core.tv"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:tv:tv-api"))
    implementation(project(":core:utils"))
    implementation(project(":feature:home:domain"))

    implementation(libs.hilt.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.tvprovider)

    ksp(libs.hilt.compiler)
}
