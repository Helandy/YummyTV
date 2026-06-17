plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.domain.library"
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)
}
