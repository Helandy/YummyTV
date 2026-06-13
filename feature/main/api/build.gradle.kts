plugins {
    id("yummytv.android.library.compose")
}

android {
    namespace = "su.afk.yummy.tv.feature.main.api"
}

dependencies {
    implementation(libs.compose.runtime)
}
