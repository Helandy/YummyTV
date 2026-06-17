plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.domain.home"
}

dependencies {
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlin.test)
}
