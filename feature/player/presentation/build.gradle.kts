plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.feature.player.presentation"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    api(project(":core:preferences"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:storage"))
    implementation(project(":feature:account:domain"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:player:api"))
    implementation(project(":feature:player:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)

    ksp(libs.hilt.compiler)
}
