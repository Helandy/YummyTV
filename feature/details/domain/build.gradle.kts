plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "su.afk.yummy.tv.domain.anime"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.javax.inject)

    testImplementation(libs.kotlin.test)
}
