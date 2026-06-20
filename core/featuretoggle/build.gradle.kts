plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.featuretoggle"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:logger"))

    implementation(libs.varioqub.config)
    implementation(libs.varioqub.appmetrica.adapter)
    implementation(libs.kotlinx.coroutines.android)
}
