plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.core.utils"
}

dependencies {
    implementation(libs.androidx.paging.runtime)
    implementation(libs.kotlinx.coroutines.android)
}
