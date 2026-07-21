plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.data.videodownload"
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:logger"))
    implementation(project(":core:preferences"))
    implementation(project(":core:storage"))
    implementation(project(":core:utils"))
    implementation(project(":feature:player:domain"))
    implementation(project(":feature:video-download:api"))
    implementation(project(":feature:video-download:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.work)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.work.runtime.ktx)
    implementation(libs.bundles.media3.player)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.media3.transformer)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    add("ksp", libs.hilt.work.compiler)
}
