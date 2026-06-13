plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.core.utils"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
