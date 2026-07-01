plugins {
    id("yummytv.android.library")
    id("yummytv.android.hilt")
}

android {
    namespace = "su.afk.yummy.tv.domain.videodownload"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
