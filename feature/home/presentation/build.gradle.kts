plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "su.afk.yummy.tv.feature.home.presentation"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":feature:details:domain"))
    api(project(":feature:home:domain"))

    implementation(project(":core:designsystem"))
    implementation(project(":core:error"))
    implementation(project(":core:navigation"))
    implementation(project(":core:storage"))
    implementation(project(":core:utils"))
    implementation(project(":feature:collection:api"))
    implementation(project(":feature:details:api"))
    implementation(project(":feature:player:api"))

    implementation(libs.bundles.compose.presentation)

    implementation(libs.hilt.android)

    ksp(libs.hilt.compiler)

    testImplementation(libs.bundles.unit.test)
}
