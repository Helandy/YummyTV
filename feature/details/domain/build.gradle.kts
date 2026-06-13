plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.domain.anime"
}

dependencies {
    implementation(libs.javax.inject)

    testImplementation(libs.kotlin.test)
}
