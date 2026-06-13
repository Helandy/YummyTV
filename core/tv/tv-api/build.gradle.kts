plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.core.tv.api"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
