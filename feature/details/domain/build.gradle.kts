plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.domain.anime"
}

dependencies {
    api(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)

    testImplementation(libs.kotlin.test)
}
