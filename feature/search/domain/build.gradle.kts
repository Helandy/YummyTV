plugins {
    id("yummytv.android.library")
}

android {
    namespace = "su.afk.yummy.tv.domain.search"
}

dependencies {
    implementation(libs.javax.inject)

    testImplementation(libs.kotlin.test)
}
