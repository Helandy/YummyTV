plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.core.analytics"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:logger"))

    implementation(libs.appmetrica.analytics)
}
