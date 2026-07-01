plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.data.videodownload"
}

dependencies {
    implementation(project(":core:storage"))
    implementation(project(":feature:video-download:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.work)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.work.runtime.ktx)
    implementation(libs.bundles.media3.player)
    implementation(libs.kotlinx.serialization.json)

    add("ksp", libs.hilt.work.compiler)
}
